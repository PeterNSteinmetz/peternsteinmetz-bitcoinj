package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.PostgresPrunedBlockStore;
import com.google.bitcoin.store.PrunedBlockStore;
import org.junit.Ignore;
import org.junit.Test;

/**
 * A Postgres implementation of the {@link AbstractFullPrunedBlockChainTest}
 */
@Ignore("enable the postgres driver dependency in the maven POM")
public class PostgresVerifiedBlockChainTest extends AbstractFullPrunedBlockChainTest
{
    // Replace these with your postgres location/credentials and remove @Ignore to test
    private static final String DB_HOSTNAME = "localhost";
    private static final String DB_NAME = "bitcoinj_test";
    private static final String DB_USERNAME = "bitcoinj";
    private static final String DB_PASSWORD = "password";
    private static final String DB_SCHEMA = "blockstore_schema";

    // whether to run the test with a schema name
    private boolean useSchema = false;

    @Override
    public PrunedBlockStore createStore(NetworkParameters params, int blockCount)
            throws BlockStoreException {
        if(useSchema) {
            return new PostgresPrunedBlockStore(params, blockCount, DB_HOSTNAME, DB_NAME, DB_USERNAME, DB_PASSWORD, DB_SCHEMA);
        }
        else {
            return new PostgresPrunedBlockStore(params, blockCount, DB_HOSTNAME, DB_NAME, DB_USERNAME, DB_PASSWORD);
        }
    }

    @Override
    public void resetStore(PrunedBlockStore store) throws BlockStoreException {
        ((PostgresPrunedBlockStore)store).resetStore();
    }

    @Test
    public void testFirst100kBlocksWithCustomSchema() throws Exception {
        boolean oldSchema = useSchema;
        useSchema = true;
        try {
            super.testFirst100KBlocks();
        } finally {
            useSchema = oldSchema;
        }
    }
}
