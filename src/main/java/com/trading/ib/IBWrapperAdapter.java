package com.trading.ib;

import com.ib.client.*;
import com.ib.client.protobuf.*;
import java.util.Set;
import java.util.Map;

public abstract class IBWrapperAdapter implements EWrapper {

    // ============================
    // --- Market Data Overrides ---
    // ============================
    @Override public void tickPrice(int tickerId, int field, double price, TickAttrib attrib) {}
    @Override public void tickSize(int tickerId, int field, Decimal size) {}
    @Override public void tickGeneric(int tickerId,int tickType,double value) {}
    @Override public void tickString(int tickerId,int tickType,String value) {}
    @Override public void tickEFP(int tickerId,int tickType,double basisPoints,String formattedBasisPoints,double totalDividends,int holdDays,String futureExpiry,double dividendImpact,double dividendsToExpiry) {}
    @Override public void tickOptionComputation(int tickerId,int field,int tickAttrib,double impliedVol,double delta,double optPrice,double pvDividend,double gamma,double vega,double theta,double undPrice) {}
    @Override public void updateMktDepth(int tickerId, int position, int operation, int side, double price, Decimal size) {}
    @Override public void updateMktDepthL2(int tickerId, int position, String marketMaker,
                                           int side, int operation, double price, Decimal size, boolean isSmartDepth) {}
    @Override public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {}
    @Override public void tickByTickAllLast(int i, int i1, long l, double v, Decimal decimal, TickAttribLast tickAttribLast, String s, String s1) {}
    @Override public void tickByTickBidAsk(int i, long l, double v, double v1, Decimal decimal, Decimal decimal1, TickAttribBidAsk tickAttribBidAsk) {}
    @Override public void tickByTickMidPoint(int i, long l, double v) {}
    @Override public void tickSnapshotEnd(int i) {}
    @Override public void tickNews(int i, long l, String s, String s1, String s2, String s3) {}
    @Override public void currentTime(long l) {}
    @Override public void currentTimeInMillis(long l) {}
    @Override public void fundamentalData(int i, String s) {}
    @Override public void deltaNeutralValidation(int i, DeltaNeutralContract deltaNeutralContract) {}

    // ============================
    // --- Orders & Executions ---
    // ============================
    @Override public void openOrder(int orderId, Contract contract, Order order, OrderState orderState) {}
    @Override public void openOrderEnd() {}
    @Override public void execDetailsEnd(int reqId) {}
    @Override public void completedOrder(Contract contract, Order order, OrderState orderState) {}
    @Override public void completedOrdersEnd() {}
    @Override public void orderBound(long l, int i, int i1) {}
    @Override public void commissionAndFeesReport(CommissionAndFeesReport commissionAndFeesReport) {}
    @Override public void nextValidId(int i) {}

    // ============================
    // --- Account & Portfolio ---
    // ============================
    @Override public void updateAccountValue(String key,String value,String currency,String accountName) {}
    @Override public void updatePortfolio(Contract contract, Decimal position, double marketPrice, double marketValue,
                                          double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {}
    @Override public void updateAccountTime(String timeStamp) {}
    @Override public void accountDownloadEnd(String accountName) {}
    @Override public void accountSummary(int reqId, String account, String tag, String value, String currency) {}
    @Override public void accountSummaryEnd(int reqId) {}
    @Override public void position(String account, Contract contract, Decimal pos, double avgCost) {}
    @Override public void positionEnd() {}
    @Override public void positionMulti(int reqId, String account, String modelCode, Contract contract, Decimal pos, double avgCost) {}
    @Override public void positionMultiEnd(int reqId) {}
    @Override public void accountUpdateMulti(int reqId, String account, String modelCode, String key, String value, String currency) {}
    @Override public void accountUpdateMultiEnd(int reqId) {}
    @Override public void managedAccounts(String s) {}
    @Override public void receiveFA(int i, String s) {}

    // ============================
    // --- Contracts & Security ---
    // ============================
    @Override public void contractDetails(int i, ContractDetails contractDetails) {}
    @Override public void bondContractDetails(int i, ContractDetails contractDetails) {}
    @Override public void contractDetailsEnd(int i) {}
    @Override public void securityDefinitionOptionalParameter(int i, String s, int i1, String s1, String s2, Set<String> set, Set<Double> set1) {}
    @Override public void securityDefinitionOptionalParameterEnd(int i) {}
    @Override public void scannerParameters(String s) {}
    @Override public void replaceFAEnd(int i, String s) {}
    @Override public void userInfo(int i, String s) {}

    // ============================
    // --- Historical Data ---
    // ============================
    @Override public void historicalData(int reqId, Bar bar) {}
    @Override public void historicalDataEnd(int reqId, String startDateStr, String endDateStr) {}
    @Override public void historicalDataUpdate(int reqId, Bar bar) {}
    @Override public void historicalTicks(int reqId, java.util.List<HistoricalTick> ticks, boolean done) {}
    @Override public void historicalTicksBidAsk(int reqId, java.util.List<HistoricalTickBidAsk> ticks, boolean done) {}
    @Override public void historicalTicksLast(int reqId, java.util.List<HistoricalTickLast> ticks, boolean done) {}
    @Override public void headTimestamp(int reqId, String headTimestamp) {}
    @Override public void histogramData(int reqId, java.util.List<HistogramEntry> items) {}
    @Override public void historicalSchedule(int reqId, String startDateTime, String endDateTime, String timeZone, java.util.List<HistoricalSession> sessions) {}

    // ============================
    // --- News & Market Info ---
    // ============================
    @Override public void updateNewsBulletin(int msgId,int msgType,String message,String origExchange) {}
    @Override public void newsProviders(NewsProvider[] newsProviders) {}
    @Override public void newsArticle(int requestId, int articleType, String articleText) {}
    @Override public void historicalNews(int requestId, String time, String providerCode, String articleId, String headline) {}
    @Override public void historicalNewsEnd(int requestId, boolean hasMore) {}
    @Override public void marketDataType(int reqId,int marketDataType) {}
    @Override public void marketRule(int marketRuleId, PriceIncrement[] priceIncrements) {}
    @Override public void pnl(int reqId, double dailyPnL, double unrealizedPnL, double realizedPnL) {}
    @Override public void pnlSingle(int reqId, Decimal pos, double dailyPnL, double unrealizedPnL, double realizedPnL, double value) {}

    // ============================
    // --- Connection & Errors ---
    // ============================
    @Override public void connectAck() {}
    @Override public void connectionClosed() {}
    @Override public void error(Exception e) {}
    @Override public void error(String str) {}
    @Override public void verifyMessageAPI(String apiData) {}
    @Override public void verifyCompleted(boolean isSuccessful, String errorText) {}
    @Override public void verifyAndAuthMessageAPI(String apiData, String xyz) {}
    @Override public void verifyAndAuthCompleted(boolean isSuccessful, String errorText) {}

    // ============================
    // --- Display & Misc ---
    // ============================
    @Override public void displayGroupList(int reqId, String groups) {}
    @Override public void displayGroupUpdated(int reqId, String contractInfo) {}
    @Override public void softDollarTiers(int reqId, SoftDollarTier[] tiers) {}
    @Override public void familyCodes(FamilyCode[] familyCodes) {}
    @Override public void symbolSamples(int reqId, ContractDescription[] contractDescriptions) {}
    @Override public void mktDepthExchanges(DepthMktDataDescription[] depthMktDataDescriptions) {}
    @Override public void smartComponents(int reqId, Map<Integer, Map.Entry<String, Character>> theMap) {}
    @Override public void rerouteMktDataReq(int i, int i1, String s) {}
    @Override public void rerouteMktDepthReq(int i, int i1, String s) {}
    @Override public void wshMetaData(int i, String s) {}
    @Override public void wshEventData(int i, String s) {}

    // ============================
    // --- Protobuf Methods ---
    // ============================
    @Override public void orderStatusProtoBuf(OrderStatusProto.OrderStatus orderStatus) {}
    @Override public void openOrderProtoBuf(OpenOrderProto.OpenOrder openOrder) {}
    @Override public void openOrdersEndProtoBuf(OpenOrdersEndProto.OpenOrdersEnd openOrdersEnd) {}
    @Override public void errorProtoBuf(ErrorMessageProto.ErrorMessage errorMessage) {}
    @Override public void execDetailsProtoBuf(ExecutionDetailsProto.ExecutionDetails executionDetails) {}
    @Override public void execDetailsEndProtoBuf(ExecutionDetailsEndProto.ExecutionDetailsEnd executionDetailsEnd) {}
}