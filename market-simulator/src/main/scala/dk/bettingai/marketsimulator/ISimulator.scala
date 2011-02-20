package dk.bettingai.marketsimulator

import ISimulator._
import scala.io._
import dk.bettingai.marketsimulator.trader._
import dk.bettingai.marketsimulator.risk._
import java.io.File
import dk.bettingai.marketsimulator.trader.ITrader._
import dk.bettingai.marketsimulator.betex.api._
import scala.collection._

/**This trait represents a simulator that processes market events, analyses traders  and returns analysis reports.
 * 
 * @author korzekwad
 *
 */
object ISimulator {

  /**
   * @param userId The betting exchange user id for registered trader.
   * @param trader Registered trader.
   *
   */
  case class RegisteredTrader(userId: Int, val trader: ITrader) extends ITrader {

    override def init(ctx: ITraderContext) = trader.init(ctx)
    def execute(ctx: ITraderContext) = trader.execute(ctx)
    override def after(ctx: ITraderContext) = trader.after(ctx)
  }

  case class SimulationReport(marketReports: List[MarketReport]) {
    /**Returns total expected profit for betting exchange user id.*/
    def totalExpectedProfit(userId: Int): Double = marketReports.foldLeft(0d)((total, marketReport) => total + marketReport.expectedProfit(userId))
    
    /**Returns total number of matched bets for userId, which were placed on all betting exchange markets.*/
    def totalMatchedBetsNum(userId: Int) = marketReports.foldLeft(0l)((total,marketReport) => total + marketReport.matchedBetsNum(userId))
    
     /**Returns total number of unmatched bets for userId, which were placed on all betting exchange markets.*/
    def totalUnmatchedBetsNum(userId: Int) = marketReports.foldLeft(0l)((total,marketReport) => total + marketReport.unmatchedBetsNum(userId))
  }

  case class MarketReport(
    val marketId: Long,
    val marketName: String,
    val eventName: String,
    traderReports: List[TraderReport]) {

    /**Returns expected profit on a market for a trader user id.*/
    def expectedProfit(userId: Int): Double = traderReports.find(tr => tr.trader.userId == userId).get.marketExpectedProfit.marketExpectedProfit
    
     /**Returns total number of matched bets for userId, which were placed on a given betting exchange market.*/
    def matchedBetsNum(userId: Int) = traderReports.find(tr => tr.trader.userId == userId).get.matchedBetsNumber
    
     /**Returns total number of unmatched bets for userId, which were placed on a given betting exchange market.*/
    def unmatchedBetsNum(userId: Int) = traderReports.find(tr => tr.trader.userId == userId).get.unmatchedBetsNumber
  }

  case class TraderReport(
    val trader: RegisteredTrader,
    /**Market expected profit based on bets and market probabilities.*/
    val marketExpectedProfit: MarketExpectedProfit,
    val matchedBetsNumber: Long,
    val unmatchedBetsNumber: Long,
    /**Labels for all chart series.*/
    val chartLabels: List[String],
    /**Key - time stamp, value - list of values for all series in the same order as labels.*/
    val chartValues: List[Tuple2[Long, List[Double]]])

  class MarketRiskReport(
    val marketId: Long,
    val marketName: String,
    val eventName: String,
    /**Market expected profit based on bets and market probabilities.*/
    val marketExpectedProfit: MarketExpectedProfit,
    val matchedBetsNumber: Long,
    val unmatchedBetsNumber: Long,
    /**Labels for all chart series.*/
    val chartLabels: List[String],
    /**Key - time stamp, value - list of values for all series in the same order as labels.*/
    val chartValues: List[Tuple2[Long, List[Double]]]) {

    override def toString() = "MarketRiskReport [marketId=%s, marketName=%s, eventName=%s, marketExpectedProfit=%s, matchedBetsNumber=%s, unmatchedBetsNumber=%s, chartLabels=%s, chartValues=%s]".format(marketId, marketName, eventName, marketExpectedProfit, matchedBetsNumber, unmatchedBetsNumber, chartLabels, chartValues)
  }

}
trait ISimulator {

  /** Processes market events, analyses traders and returns analysis reports.
   * 
   * @param marketDataContains market events that the market simulation is executed for. Key - marketId, value - market events
   * @param traders Traders to analyse, all they are analysed on the same time, so they compete against each other
   * @param p Progress listener. Value between 0% and 100% is passed as an function argument.
   */
  def runSimulation(marketData: Map[Long, File], traders: List[ITrader], p: (Int) => Unit): SimulationReport

  /**Registers new trader and return trader context. 
   * This context can be used to trigger some custom traders that are registered manually by a master trader, 
   * e.g. when testing some evolution algorithms for which more than one trader is required.
   * @return trader context
   */
  def registerTrader(market: IMarket): ITraderContext
}