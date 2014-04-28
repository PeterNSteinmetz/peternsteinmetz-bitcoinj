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
 * <p>Provides an abstract base class for {@link com.google.bitcoin.store.PrunedBlockStore}s
 * which use an SQL database for storage.</p>
 */
public abstract class AbstractSqlPrunedBlockStore implements PrunedBlockStore {

  /**
   * <p>Begins/Commits/Aborts a database transaction.</p>
   *
   * <p>If abortDatabaseBatchWrite() is called by the same thread that called beginDatabaseBatchWrite(),
   * any data writes between this call and abortDatabaseBatchWrite() made by the same thread
   * should be discarded.</p>
   *
   * <p>Furthermore, any data written after a call to beginDatabaseBatchWrite() should not be readable
   * by any other threads until commitDatabaseBatchWrite() has been called by this thread.
   * Multiple calls to beginDatabaseBatchWrite() in any given thread should be ignored and treated as one call.</p>
   */
  @Override
  public abstract void beginBatchWrite() throws BlockStoreException;
  @Override
  public abstract void commitBatchWrite() throws BlockStoreException;
  @Override public abstract void abortBatchWrite() throws BlockStoreException;

}
