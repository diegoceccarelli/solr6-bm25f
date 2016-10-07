/**
 *  Copyright 2012 Diego Ceccarelli
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package lucene4ir.bm25f;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.surround.parser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBM25FQuery extends LuceneTestCase {
  
  IndexSearcher searcherUnderTest;
  RandomIndexWriter indexWriterUnderTest;
  IndexReader indexReaderUnderTest;
  Directory dirUnderTest;
  
  @Before
  public void setupIndex() throws IOException {
    dirUnderTest = newDirectory();
    
    indexWriterUnderTest = new RandomIndexWriter(random(), dirUnderTest);
    final Document doc = new Document();
    doc.add(newStringField("id", "0", Store.YES));
    doc.add(newStringField("title", "leonardo da vinci", Store.YES));
    doc.add(newStringField("author", "leonardo da", Store.YES));
    doc.add(newStringField("description", "VIDEO", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);
    
    doc.add(newStringField("id", "0", Store.YES));
    doc.add(newStringField("title", "leonardo ", Store.YES));
    doc.add(newStringField("author", "leonardo da vinci ", Store.YES));
    doc.add(newStringField("description", "IMAGE", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);
    
    doc.add(newStringField("id", "0", Store.YES));
    doc.add(newStringField("title", "leonardo da vinci", Store.YES));
    doc.add(newStringField("author", "leonardo da", Store.YES));
    doc.add(newStringField("description", "VIDEO", Store.YES));
    
    indexWriterUnderTest.addDocument(doc);
    
    indexReaderUnderTest = indexWriterUnderTest.getReader();
    searcherUnderTest = newSearcher(indexReaderUnderTest);
  }
  
  @After
  public void closeStuff() throws IOException {
    indexReaderUnderTest.close();
    indexWriterUnderTest.close();
    dirUnderTest.close();
    
  }
  
  public ScoreDoc getResults(String field, String query) {
    final Query q = 
    // we should get the "brown" docs backwards first (ie the nworb)
    final TopDocs t = searcherUnderTest.search(q, 10);
    final ScoreDoc[] docs = t.scoreDocs;
  }
  
  @Test
  public void testSearch() throws IOException, QueryNodeException {
    final Query q = new StandardQueryParser().parse("how", "field");
    // we should get the "brown" docs backwards first (ie the nworb)
    final TopDocs t = searcherUnderTest.search(q, 10);
    final ScoreDoc[] docs = t.scoreDocs;
    assertEquals(1, docs.length);
    
  }
  
}
