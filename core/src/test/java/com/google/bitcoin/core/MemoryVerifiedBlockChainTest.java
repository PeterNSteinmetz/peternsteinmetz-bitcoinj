package com.google.bitcoin.core;

import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.PrunedBlockStore;
import com.google.bitcoin.store.MemoryPrunedBlockStore;

/**
 * A MemoryStore implementation of the FullPrunedBlockStoreTest
 */
public class MemoryVerifiedBlockChainTest extends AbstractFullPrunedBlockChainTest
{
    @Override
    public PrunedBlockStore createStore(NetworkParameters params, int blockCount) throws BlockStoreException
    {
        return new MemoryPrunedBlockStore(params, blockCount);
    }

    @Override
    public void resetStore(PrunedBlockStore store) throws BlockStoreException
    {
        //No-op for memory store, because it's not persistent
    }
}
