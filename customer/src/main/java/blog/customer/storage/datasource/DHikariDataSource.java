package blog.customer.storage.datasource;

import com.zaxxer.hikari.HikariDataSource;

public class DHikariDataSource extends HikariDataSource {
    public DHikariDataSource() {
        super();
        super.addDataSourceProperty("useSSL", "false");
        super.addDataSourceProperty("tcpRcvBuf", "524288");
        super.addDataSourceProperty("tcpSndBuf", "524288");
        super.addDataSourceProperty("cachePrepStmts", "true");
        super.addDataSourceProperty("prepStmtCacheSize", "256");
        super.addDataSourceProperty("allowMultiQueries", "true");
        super.addDataSourceProperty("maintainTimeStats", "false");
        super.addDataSourceProperty("useServerPrepStmts", "false");
        super.addDataSourceProperty("elideSetAutoCommits", "true");
        super.addDataSourceProperty("useLocalSessionState", "true");
        super.addDataSourceProperty("prepStmtCacheSqlLimit", "32768");
        super.addDataSourceProperty("nullCatalogMeansCurrent", "true");
        super.addDataSourceProperty("rewriteBatchedStatements", "true");
        super.addDataSourceProperty("useOldAliasMetadataBehavior", "true");
    }
}
