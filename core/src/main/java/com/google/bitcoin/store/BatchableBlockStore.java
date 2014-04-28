/*
 * Copyright 2014 Peter N. Steinmetz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.bitcoin.store;

/**
 * An implementor of BatchableBlockStore saves {@link com.google.bitcoin.core.StoredBlock}s to disk
 * as a {@link com.google.bitcoin.store.BlockStore}.
 *
 * It provides methods for batching changes to the @{link BlockStore} with the ability
 * to write batches and abort them.
 *
 * BatchableBlockStores are thread safe.
 */
public interface BatchableBlockStore extends BlockStore {

  /**
   * <p>Begins/Commits/Aborts a batch of changes to the {@link com.google.bitcoin.store.BlockStore}.</p>
   *
   * <p>If abortBatchWrite() is called by the same thread that called beginBatchWrite(),
   * any data writes between this call and abortBatchWrite() made by the same thread
   * should be discarded.</p>
   *
   * <p>Furthermore, any data written after a call to beginBatchWrite() should not be readable
   * by any other threads until commitBatchWrite() has been called by this thread.
   * Multiple calls to beginBatchWrite() in any given thread should be ignored and treated as one call.</p>
   */
  void beginBatchWrite() throws BlockStoreException;
  void commitBatchWrite() throws BlockStoreException;
  void abortBatchWrite() throws BlockStoreException;

}
