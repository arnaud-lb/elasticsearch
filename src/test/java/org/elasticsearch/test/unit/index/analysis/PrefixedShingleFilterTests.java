package org.elasticsearch.test.unit.index.analysis;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.elasticsearch.index.analysis.PrefixedShingleFilter;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

public class PrefixedShingleFilterTests extends BaseTokenStreamTestCase {

  public class TestTokenStream extends TokenStream {

    protected int index = 0;
    protected Token[] testToken;
    
    private CharTermAttribute termAtt;
    private OffsetAttribute offsetAtt;
    private PositionIncrementAttribute posIncrAtt;
    private TypeAttribute typeAtt;

    public TestTokenStream(Token[] testToken) {
      super();
      this.testToken = testToken;
      this.termAtt = addAttribute(CharTermAttribute.class);
      this.offsetAtt = addAttribute(OffsetAttribute.class);
      this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
      this.typeAtt = addAttribute(TypeAttribute.class);
    }

    @Override
    public final boolean incrementToken() throws IOException {
      clearAttributes();
      if (index < testToken.length) {
        Token t = testToken[index++];
        termAtt.copyBuffer(t.buffer(), 0, t.length());
        offsetAtt.setOffset(t.startOffset(), t.endOffset());
        posIncrAtt.setPositionIncrement(t.getPositionIncrement());
        typeAtt.setType(TypeAttribute.DEFAULT_TYPE);
        return true;
      } else {
        return false;
      }
    }
  }

  public static final Token[] TEST_TOKEN = new Token[] {
      createToken("prefix", 0, 6),
      createToken("please", 7, 13),
      createToken("divide", 14, 20),
      createToken("this", 21, 25),
      createToken("sentence", 26, 34),
      createToken("into", 35, 39),
      createToken("shingles", 40, 46),
  };

  public static final Token[] UNI_GRAM_TOKENS = new Token[] {
      createToken("prefix please", 0, 6),
      createToken("prefix divide", 7, 13),
      createToken("prefix this", 14, 18),
      createToken("prefix sentence", 19, 27),
      createToken("prefix into", 28, 32),
      createToken("prefix shingles", 33, 39),
  };

  public static final int[] UNIGRAM_ONLY_POSITION_INCREMENTS = new int[] {
    1, 1, 1, 1, 1, 1
  };

  public static final String[] UNIGRAM_ONLY_TYPES = new String[] {
    "word", "word", "word", "word", "word", "word"
  };

  public static Token[] testTokenWithHoles;

  public static final Token[] BI_GRAM_TOKENS = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide", 0, 13),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this", 7, 18),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix into", 28, 32),
    createToken("prefix into shingles", 28, 39),
    createToken("prefix shingles", 33, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };

  public static final String[] BI_GRAM_TYPES = new String[] {
    "word", "shingle", "word", "shingle", "word", "shingle", "word",
    "shingle", "word", "shingle", "word"
  };

  public static final Token[] BI_GRAM_TOKENS_WITH_HOLES = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide", 0, 13),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide _", 7, 19),
    createToken("prefix _ sentence", 19, 27),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence _", 19, 33),
    createToken("prefix _ shingles", 33, 39),
    createToken("prefix shingles", 33, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_WITH_HOLES = new int[] {
    1, 0, 1, 0, 1, 1, 0, 1, 1
  };

  private static final String[] BI_GRAM_TYPES_WITH_HOLES = {
    "word", "shingle", 
    "word", "shingle", "shingle", "word", "shingle", "shingle", "word"
  };

  public static final Token[] BI_GRAM_TOKENS_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please divide", 0, 13),
    createToken("prefix divide this", 7, 18),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix into shingles", 28, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS = new int[] {
    1, 1, 1, 1, 1
  };

  public static final String[] BI_GRAM_TYPES_WITHOUT_UNIGRAMS = new String[] {
    "shingle", "shingle", "shingle", "shingle", "shingle"
  };

  public static final Token[] BI_GRAM_TOKENS_WITH_HOLES_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please divide", 0, 13),
    createToken("prefix divide _", 7, 19),
    createToken("prefix _ sentence", 19, 27),
    createToken("prefix sentence _", 19, 33),
    createToken("prefix _ shingles", 33, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_WITH_HOLES_WITHOUT_UNIGRAMS = new int[] {
    1, 1, 1, 1, 1, 1
  };


  public static final Token[] TEST_SINGLE_TOKEN = new Token[] {
    createToken("prefix", 0, 6),
    createToken("please", 21, 27)
  };

  public static final Token[] SINGLE_TOKEN = new Token[] {
    createToken("prefix please", 0, 6)
  };

  public static final int[] SINGLE_TOKEN_INCREMENTS = new int[] {
    1
  };

  public static final String[] SINGLE_TOKEN_TYPES = new String[] {
    "word"
  };

  public static final Token[] EMPTY_TOKEN_ARRAY = new Token[] {
  };

  public static final int[] EMPTY_TOKEN_INCREMENTS_ARRAY = new int[] {
  };

  public static final String[] EMPTY_TOKEN_TYPES_ARRAY = new String[] {
  };

  public static final Token[] TRI_GRAM_TOKENS = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into", 28, 32),
    createToken("prefix into shingles", 28, 39),
    createToken("prefix shingles", 33, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };

  public static final String[] TRI_GRAM_TYPES = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into shingles", 28, 39),
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1
  };
  
  public static final String[] TRI_GRAM_TYPES_WITHOUT_UNIGRAMS = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle",
  };
  
  public static final Token[] FOUR_GRAM_TOKENS = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix this sentence into shingles", 14, 39),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into", 28, 32),
    createToken("prefix into shingles", 28, 39),
    createToken("prefix shingles", 33, 39)
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS = new int[] {
    1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1
  };

  public static final String[] FOUR_GRAM_TYPES = new String[] {
    "word", "shingle", "shingle", "shingle",
    "word", "shingle", "shingle", "shingle",
    "word", "shingle", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this sentence", 14, 27),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix this sentence into shingles", 14, 39),
    createToken("prefix sentence into", 19, 32),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into shingles", 28, 39),
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };
  
  public static final String[] FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",

  };

  public static final Token[] TRI_GRAM_TOKENS_MIN_TRI_GRAM = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into", 28, 32),
    createToken("prefix shingles", 33, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_MIN_TRI_GRAM = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1, 1
  };

  public static final String[] TRI_GRAM_TYPES_MIN_TRI_GRAM = new String[] {
    "word", "shingle",
    "word", "shingle",
    "word", "shingle",
    "word", "shingle",
    "word",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new Token[] {
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix sentence into shingles", 19, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new int[] {
    1, 1, 1, 1
  };
  
  public static final String[] TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new String[] {
    "shingle",
    "shingle",
    "shingle",
    "shingle"
  };
  
  public static final Token[] FOUR_GRAM_TOKENS_MIN_TRI_GRAM = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix this sentence into shingles", 14, 39),
    createToken("prefix sentence", 19, 27),
    createToken("prefix sentence into shingles", 19, 39),
    createToken("prefix into", 28, 32),
    createToken("prefix shingles", 33, 39)
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS_MIN_TRI_GRAM = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1
  };

  public static final String[] FOUR_GRAM_TYPES_MIN_TRI_GRAM = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word",
    "word"
  };
  
  public static final Token[] FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new Token[] {
    createToken("prefix please divide this", 0, 18),
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide this sentence", 7, 27),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this sentence into", 14, 32),
    createToken("prefix this sentence into shingles", 14, 39),
    createToken("prefix sentence into shingles", 19, 39),
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new int[] {
    1, 0, 1, 0, 1, 0, 1
  };
  
  public static final String[] FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_TRI_GRAM = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle"
  };
  
  public static final Token[] FOUR_GRAM_TOKENS_MIN_FOUR_GRAM = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this", 14, 18),
    createToken("prefix this sentence into shingles", 14, 39),
    createToken("prefix sentence", 19, 27),
    createToken("prefix into", 28, 32),
    createToken("prefix shingles", 33, 39)
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS_MIN_FOUR_GRAM = new int[] {
    1, 0, 1, 0, 1, 0, 1, 1, 1
  };

  public static final String[] FOUR_GRAM_TYPES_MIN_FOUR_GRAM = new String[] {
    "word", "shingle",
    "word", "shingle",
    "word", "shingle",
    "word",
    "word",
    "word"
  };
  
  public static final Token[] FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM = new Token[] {
    createToken("prefix please divide this sentence", 0, 27),
    createToken("prefix divide this sentence into", 7, 32),
    createToken("prefix this sentence into shingles", 14, 39),
  };

  public static final int[] FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM = new int[] {
    1, 1, 1
  };
  
  public static final String[] FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM = new String[] {
    "shingle",
    "shingle",
    "shingle"
  };

  public static final Token[] BI_GRAM_TOKENS_NO_SEPARATOR = new Token[] {
    createToken("prefixplease", 0, 6),
    createToken("prefixpleasedivide", 0, 13),
    createToken("prefixdivide", 7, 13),
    createToken("prefixdividethis", 7, 18),
    createToken("prefixthis", 14, 18),
    createToken("prefixthissentence", 14, 27),
    createToken("prefixsentence", 19, 27),
    createToken("prefixsentenceinto", 19, 32),
    createToken("prefixinto", 28, 32),
    createToken("prefixintoshingles", 28, 39),
    createToken("prefixshingles", 33, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_NO_SEPARATOR = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };

  public static final String[] BI_GRAM_TYPES_NO_SEPARATOR = new String[] {
    "word", "shingle", "word", "shingle", "word", "shingle", "word",
    "shingle", "word", "shingle", "word"
  };

  public static final Token[] BI_GRAM_TOKENS_WITHOUT_UNIGRAMS_NO_SEPARATOR = new Token[] {
    createToken("prefixpleasedivide", 0, 13),
    createToken("prefixdividethis", 7, 18),
    createToken("prefixthissentence", 14, 27),
    createToken("prefixsentenceinto", 19, 32),
    createToken("prefixintoshingles", 28, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_NO_SEPARATOR = new int[] {
    1, 1, 1, 1, 1
  };

  public static final String[] BI_GRAM_TYPES_WITHOUT_UNIGRAMS_NO_SEPARATOR = new String[] {
    "shingle", "shingle", "shingle", "shingle", "shingle"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_NO_SEPARATOR = new Token[] {
    createToken("prefixplease", 0, 6),
    createToken("prefixpleasedivide", 0, 13),
    createToken("prefixpleasedividethis", 0, 18),
    createToken("prefixdivide", 7, 13),
    createToken("prefixdividethis", 7, 18),
    createToken("prefixdividethissentence", 7, 27),
    createToken("prefixthis", 14, 18),
    createToken("prefixthissentence", 14, 27),
    createToken("prefixthissentenceinto", 14, 32),
    createToken("prefixsentence", 19, 27),
    createToken("prefixsentenceinto", 19, 32),
    createToken("prefixsentenceintoshingles", 19, 39),
    createToken("prefixinto", 28, 32),
    createToken("prefixintoshingles", 28, 39),
    createToken("prefixshingles", 33, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_NO_SEPARATOR = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };

  public static final String[] TRI_GRAM_TYPES_NO_SEPARATOR = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_NO_SEPARATOR = new Token[] {
    createToken("prefixpleasedivide", 0, 13),
    createToken("prefixpleasedividethis", 0, 18),
    createToken("prefixdividethis", 7, 18),
    createToken("prefixdividethissentence", 7, 27),
    createToken("prefixthissentence", 14, 27),
    createToken("prefixthissentenceinto", 14, 32),
    createToken("prefixsentenceinto", 19, 32),
    createToken("prefixsentenceintoshingles", 19, 39),
    createToken("prefixintoshingles", 28, 39),
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_NO_SEPARATOR = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1
  };
  
  public static final String[] TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_NO_SEPARATOR = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle",
  };

  public static final Token[] BI_GRAM_TOKENS_ALT_SEPARATOR = new Token[] {
    createToken("prefix<SEP>please", 0, 6),
    createToken("prefix<SEP>please<SEP>divide", 0, 13),
    createToken("prefix<SEP>divide", 7, 13),
    createToken("prefix<SEP>divide<SEP>this", 7, 18),
    createToken("prefix<SEP>this", 14, 18),
    createToken("prefix<SEP>this<SEP>sentence", 14, 27),
    createToken("prefix<SEP>sentence", 19, 27),
    createToken("prefix<SEP>sentence<SEP>into", 19, 32),
    createToken("prefix<SEP>into", 28, 32),
    createToken("prefix<SEP>into<SEP>shingles", 28, 39),
    createToken("prefix<SEP>shingles", 33, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_ALT_SEPARATOR = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };

  public static final String[] BI_GRAM_TYPES_ALT_SEPARATOR = new String[] {
    "word", "shingle", "word", "shingle", "word", "shingle", "word",
    "shingle", "word", "shingle", "word"
  };

  public static final Token[] BI_GRAM_TOKENS_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new Token[] {
    createToken("prefix<SEP>please<SEP>divide", 0, 13),
    createToken("prefix<SEP>divide<SEP>this", 7, 18),
    createToken("prefix<SEP>this<SEP>sentence", 14, 27),
    createToken("prefix<SEP>sentence<SEP>into", 19, 32),
    createToken("prefix<SEP>into<SEP>shingles", 28, 39),
  };

  public static final int[] BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new int[] {
    1, 1, 1, 1, 1
  };

  public static final String[] BI_GRAM_TYPES_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new String[] {
    "shingle", "shingle", "shingle", "shingle", "shingle"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_ALT_SEPARATOR = new Token[] {
    createToken("prefix<SEP>please", 0, 6),
    createToken("prefix<SEP>please<SEP>divide", 0, 13),
    createToken("prefix<SEP>please<SEP>divide<SEP>this", 0, 18),
    createToken("prefix<SEP>divide", 7, 13),
    createToken("prefix<SEP>divide<SEP>this", 7, 18),
    createToken("prefix<SEP>divide<SEP>this<SEP>sentence", 7, 27),
    createToken("prefix<SEP>this", 14, 18),
    createToken("prefix<SEP>this<SEP>sentence", 14, 27),
    createToken("prefix<SEP>this<SEP>sentence<SEP>into", 14, 32),
    createToken("prefix<SEP>sentence", 19, 27),
    createToken("prefix<SEP>sentence<SEP>into", 19, 32),
    createToken("prefix<SEP>sentence<SEP>into<SEP>shingles", 19, 39),
    createToken("prefix<SEP>into", 28, 32),
    createToken("prefix<SEP>into<SEP>shingles", 28, 39),
    createToken("prefix<SEP>shingles", 33, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_ALT_SEPARATOR = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };

  public static final String[] TRI_GRAM_TYPES_ALT_SEPARATOR = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new Token[] {
    createToken("prefix<SEP>please<SEP>divide", 0, 13),
    createToken("prefix<SEP>please<SEP>divide<SEP>this", 0, 18),
    createToken("prefix<SEP>divide<SEP>this", 7, 18),
    createToken("prefix<SEP>divide<SEP>this<SEP>sentence", 7, 27),
    createToken("prefix<SEP>this<SEP>sentence", 14, 27),
    createToken("prefix<SEP>this<SEP>sentence<SEP>into", 14, 32),
    createToken("prefix<SEP>sentence<SEP>into", 19, 32),
    createToken("prefix<SEP>sentence<SEP>into<SEP>shingles", 19, 39),
    createToken("prefix<SEP>into<SEP>shingles", 28, 39),
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new int[] {
    1, 0, 1, 0, 1, 0, 1, 0, 1
  };
  
  public static final String[] TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_ALT_SEPARATOR = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle",
  };

  public static final Token[] TRI_GRAM_TOKENS_NULL_SEPARATOR = new Token[] {
    createToken("prefixplease", 0, 6),
    createToken("prefixpleasedivide", 0, 13),
    createToken("prefixpleasedividethis", 0, 18),
    createToken("prefixdivide", 7, 13),
    createToken("prefixdividethis", 7, 18),
    createToken("prefixdividethissentence", 7, 27),
    createToken("prefixthis", 14, 18),
    createToken("prefixthissentence", 14, 27),
    createToken("prefixthissentenceinto", 14, 32),
    createToken("prefixsentence", 19, 27),
    createToken("prefixsentenceinto", 19, 32),
    createToken("prefixsentenceintoshingles", 19, 39),
    createToken("prefixinto", 28, 32),
    createToken("prefixintoshingles", 28, 39),
    createToken("prefixshingles", 33, 39)
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_NULL_SEPARATOR = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };

  public static final String[] TRI_GRAM_TYPES_NULL_SEPARATOR = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TEST_TOKEN_POS_INCR_EQUAL_TO_N = new Token[] {
    createToken("prefix", 0, 6),
    createToken("please", 28, 34),
    createToken("divide", 35, 41),
    createToken("this", 42, 46),
    createToken("sentence", 57, 65, 3),
    createToken("into", 66, 70),
    createToken("shingles", 71, 77),
  };

  public static final Token[] TRI_GRAM_TOKENS_POS_INCR_EQUAL_TO_N = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide", 7, 13),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this _", 7, 29),
    createToken("prefix this", 14, 18),
    createToken("prefix this _", 14, 29),
    createToken("prefix this _ _", 14, 29),
    createToken("prefix _ _ sentence", 29, 37),
    createToken("prefix _ sentence", 29, 37),
    createToken("prefix _ sentence into", 29, 42),
    createToken("prefix sentence", 29, 37),
    createToken("prefix sentence into", 29, 42),
    createToken("prefix sentence into shingles", 29, 49),
    createToken("prefix into", 38, 42),
    createToken("prefix into shingles", 38, 49),
    createToken("prefix shingles", 43, 49)
  };
  
  public static final int[] TRI_GRAM_POSITION_INCREMENTS_POS_INCR_EQUAL_TO_N = new int[] {
    1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1
  };
  
  public static final String[] TRI_GRAM_TYPES_POS_INCR_EQUAL_TO_N = new String[] {
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "shingle", "shingle", "shingle", "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please divide", 0, 13),
    createToken("prefix please divide this", 0, 18),
    createToken("prefix divide this", 7, 18),
    createToken("prefix divide this _", 7, 29),
    createToken("prefix this _", 14, 29),
    createToken("prefix this _ _", 14, 29),
    createToken("prefix _ _ sentence", 29, 37),
    createToken("prefix _ sentence", 29, 37),
    createToken("prefix _ sentence into", 29, 42),
    createToken("prefix sentence into", 29, 42),
    createToken("prefix sentence into shingles", 29, 49),
    createToken("prefix into shingles", 38, 49),
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS = new int[] {
    1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1
  };

  public static final String[] TRI_GRAM_TYPES_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle", "shingle",
    "shingle", "shingle",
    "shingle",
  };

  public static final Token[] TEST_TOKEN_POS_INCR_GREATER_THAN_N = new Token[] {
    createToken("prefix", 0, 6),
    createToken("please", 21, 27),
    createToken("divide", 78, 84, 8),
    createToken("this", 85, 89),
    createToken("sentence", 90, 98),
    createToken("into", 99, 103),
    createToken("shingles", 104, 110),
  };
  
  public static final Token[] TRI_GRAM_TOKENS_POS_INCR_GREATER_THAN_N = new Token[] {
    createToken("prefix please", 0, 6),
    createToken("prefix please _", 0, 57),
    createToken("prefix please _ _", 0, 57),
    createToken("prefix _ _ divide", 57, 63),
    createToken("prefix _ divide", 57, 63),
    createToken("prefix _ divide this", 57, 68),
    createToken("prefix divide", 57, 63),
    createToken("prefix divide this", 57, 68),
    createToken("prefix divide this sentence", 57, 77),
    createToken("prefix this", 64, 68),
    createToken("prefix this sentence", 64, 77),
    createToken("prefix this sentence into", 64, 82),
    createToken("prefix sentence", 69, 77),
    createToken("prefix sentence into", 69, 82),
    createToken("prefix sentence into shingles", 69, 89),
    createToken("prefix into", 78, 82),
    createToken("prefix into shingles", 78, 89),
    createToken("prefix shingles", 83, 89)
  };
  
  public static final int[] TRI_GRAM_POSITION_INCREMENTS_POS_INCR_GREATER_THAN_N = new int[] {
    1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1
  };
  public static final String[] TRI_GRAM_TYPES_POS_INCR_GREATER_THAN_N = new String[] {
    "word", "shingle", "shingle",
    "shingle",
    "shingle", "shingle", 
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle", "shingle",
    "word", "shingle",
    "word"
  };
  
  public static final Token[] TRI_GRAM_TOKENS_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS = new Token[] {
    createToken("prefix please _", 0, 57),
    createToken("prefix please _ _", 0, 57),
    createToken("prefix _ _ divide", 57, 63),
    createToken("prefix _ divide", 57, 63),
    createToken("prefix _ divide this", 57, 68),
    createToken("prefix divide this", 57, 68),
    createToken("prefix divide this sentence", 57, 77),
    createToken("prefix this sentence", 64, 77),
    createToken("prefix this sentence into", 64, 82),
    createToken("prefix sentence into", 69, 82),
    createToken("prefix sentence into shingles", 69, 89),
    createToken("prefix into shingles", 78, 89),
  };

  public static final int[] TRI_GRAM_POSITION_INCREMENTS_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS = new int[] {
    1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1
  };

  public static final String[] TRI_GRAM_TYPES_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS = new String[] {
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle",
    "shingle", "shingle", "shingle", "shingle", "shingle",
    "shingle",
  };

  @Override
  @BeforeClass
  public void setUp() throws Exception {
    super.setUp();
    testTokenWithHoles = new Token[] {
      createToken("prefix", 0, 6),
      createToken("please", 21, 27),
      createToken("divide", 28, 34),
      createToken("sentence", 40, 48, 2),
      createToken("shingles", 54, 60, 2),
    };
  }

  /*
   * Class under test for void PrefixedShingleFilter(TokenStream, int)
   */
  @Test
  public void testBiGramFilter() throws IOException {
    this.shingleFilterTest(2, TEST_TOKEN, BI_GRAM_TOKENS,
                           BI_GRAM_POSITION_INCREMENTS, BI_GRAM_TYPES,
                           true);
  }

  @Test
  public void testBiGramFilterWithHoles() throws IOException {
    this.shingleFilterTest(2, testTokenWithHoles, BI_GRAM_TOKENS_WITH_HOLES,
                           BI_GRAM_POSITION_INCREMENTS_WITH_HOLES, 
                           BI_GRAM_TYPES_WITH_HOLES, 
                           true);
  }

  @Test
  public void testBiGramFilterWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, TEST_TOKEN, BI_GRAM_TOKENS_WITHOUT_UNIGRAMS,
                           BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS, BI_GRAM_TYPES_WITHOUT_UNIGRAMS,
                           false);
  }

  @Test
  public void testBiGramFilterWithHolesWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, testTokenWithHoles, BI_GRAM_TOKENS_WITH_HOLES_WITHOUT_UNIGRAMS,
                           BI_GRAM_POSITION_INCREMENTS_WITH_HOLES_WITHOUT_UNIGRAMS, BI_GRAM_TYPES_WITHOUT_UNIGRAMS,
                           false);
  }

  @Test
  public void testBiGramFilterWithSingleToken() throws IOException {
    this.shingleFilterTest(2, TEST_SINGLE_TOKEN, SINGLE_TOKEN,
                           SINGLE_TOKEN_INCREMENTS, SINGLE_TOKEN_TYPES,
                           true);
  }

  @Test
  public void testBiGramFilterWithSingleTokenWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, TEST_SINGLE_TOKEN, EMPTY_TOKEN_ARRAY,
                           EMPTY_TOKEN_INCREMENTS_ARRAY, EMPTY_TOKEN_TYPES_ARRAY,
                           false);
  }

  @Test
  public void testBiGramFilterWithEmptyTokenStream() throws IOException {
    this.shingleFilterTest(2, EMPTY_TOKEN_ARRAY, EMPTY_TOKEN_ARRAY,
                           EMPTY_TOKEN_INCREMENTS_ARRAY, EMPTY_TOKEN_TYPES_ARRAY,
                           true);
  }

  @Test
  public void testBiGramFilterWithEmptyTokenStreamWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, EMPTY_TOKEN_ARRAY, EMPTY_TOKEN_ARRAY,
                           EMPTY_TOKEN_INCREMENTS_ARRAY, EMPTY_TOKEN_TYPES_ARRAY,
                           false);
  }

  @Test
  public void testTriGramFilter() throws IOException {
    this.shingleFilterTest(3, TEST_TOKEN, TRI_GRAM_TOKENS,
                           TRI_GRAM_POSITION_INCREMENTS, TRI_GRAM_TYPES,
                           true);
  }
  
  @Test
  public void testTriGramFilterWithoutUnigrams() throws IOException {
    this.shingleFilterTest(3, TEST_TOKEN, TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS,
                           TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS, TRI_GRAM_TYPES_WITHOUT_UNIGRAMS,
                           false);
  }
  
  @Test
  public void testFourGramFilter() throws IOException {
    this.shingleFilterTest(4, TEST_TOKEN, FOUR_GRAM_TOKENS,
        FOUR_GRAM_POSITION_INCREMENTS, FOUR_GRAM_TYPES,
                           true);
  }
  
  @Test
  public void testFourGramFilterWithoutUnigrams() throws IOException {
    this.shingleFilterTest(4, TEST_TOKEN, FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS,
        FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS,
        FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS, false);
  }
  
  
  @Test
  public void testTriGramFilterMinTriGram() throws IOException {
    this.shingleFilterTest(3, 3, TEST_TOKEN, TRI_GRAM_TOKENS_MIN_TRI_GRAM,
                           TRI_GRAM_POSITION_INCREMENTS_MIN_TRI_GRAM,
                           TRI_GRAM_TYPES_MIN_TRI_GRAM,
                           true);
  }
  
  @Test
  public void testTriGramFilterWithoutUnigramsMinTriGram() throws IOException {
    this.shingleFilterTest(3, 3, TEST_TOKEN, 
                           TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM,
                           TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM, 
                           TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_TRI_GRAM,
                           false);
  }
  
  @Test
  public void testFourGramFilterMinTriGram() throws IOException {
    this.shingleFilterTest(3, 4, TEST_TOKEN, FOUR_GRAM_TOKENS_MIN_TRI_GRAM,
                           FOUR_GRAM_POSITION_INCREMENTS_MIN_TRI_GRAM, 
                           FOUR_GRAM_TYPES_MIN_TRI_GRAM,
                           true);
  }
  
  @Test
  public void testFourGramFilterWithoutUnigramsMinTriGram() throws IOException {
    this.shingleFilterTest(3, 4, TEST_TOKEN, 
                           FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM,
                           FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_TRI_GRAM,
                           FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_TRI_GRAM, false);
  }

  @Test
  public void testFourGramFilterMinFourGram() throws IOException {
    this.shingleFilterTest(4, 4, TEST_TOKEN, FOUR_GRAM_TOKENS_MIN_FOUR_GRAM,
                           FOUR_GRAM_POSITION_INCREMENTS_MIN_FOUR_GRAM, 
                           FOUR_GRAM_TYPES_MIN_FOUR_GRAM,
                           true);
  }
  
  @Test
  public void testFourGramFilterWithoutUnigramsMinFourGram() throws IOException {
    this.shingleFilterTest(4, 4, TEST_TOKEN, 
                           FOUR_GRAM_TOKENS_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM,
                           FOUR_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM,
                           FOUR_GRAM_TYPES_WITHOUT_UNIGRAMS_MIN_FOUR_GRAM, false);
  }
 
  @Test
  public void testBiGramFilterNoSeparator() throws IOException {
    this.shingleFilterTest("", 2, 2, TEST_TOKEN, BI_GRAM_TOKENS_NO_SEPARATOR,
                           BI_GRAM_POSITION_INCREMENTS_NO_SEPARATOR, 
                           BI_GRAM_TYPES_NO_SEPARATOR, true);
  }

  @Test
  public void testBiGramFilterWithoutUnigramsNoSeparator() throws IOException {
    this.shingleFilterTest("", 2, 2, TEST_TOKEN, 
                           BI_GRAM_TOKENS_WITHOUT_UNIGRAMS_NO_SEPARATOR,
                           BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_NO_SEPARATOR, 
                           BI_GRAM_TYPES_WITHOUT_UNIGRAMS_NO_SEPARATOR,
                           false);
  }
  @Test
  public void testTriGramFilterNoSeparator() throws IOException {
    this.shingleFilterTest("", 2, 3, TEST_TOKEN, TRI_GRAM_TOKENS_NO_SEPARATOR,
                           TRI_GRAM_POSITION_INCREMENTS_NO_SEPARATOR, 
                           TRI_GRAM_TYPES_NO_SEPARATOR, true);
  }
  
  @Test
  public void testTriGramFilterWithoutUnigramsNoSeparator() throws IOException {
    this.shingleFilterTest("", 2, 3, TEST_TOKEN, 
                           TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_NO_SEPARATOR,
                           TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_NO_SEPARATOR,
                           TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_NO_SEPARATOR, false);
  }
  
  @Test
  public void testBiGramFilterAltSeparator() throws IOException {
    this.shingleFilterTest("<SEP>", 2, 2, TEST_TOKEN, BI_GRAM_TOKENS_ALT_SEPARATOR,
                           BI_GRAM_POSITION_INCREMENTS_ALT_SEPARATOR, 
                           BI_GRAM_TYPES_ALT_SEPARATOR, true);
  }

  @Test
  public void testBiGramFilterWithoutUnigramsAltSeparator() throws IOException {
    this.shingleFilterTest("<SEP>", 2, 2, TEST_TOKEN, 
                           BI_GRAM_TOKENS_WITHOUT_UNIGRAMS_ALT_SEPARATOR,
                           BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_ALT_SEPARATOR, 
                           BI_GRAM_TYPES_WITHOUT_UNIGRAMS_ALT_SEPARATOR,
                           false);
  }
  @Test
  public void testTriGramFilterAltSeparator() throws IOException {
    this.shingleFilterTest("<SEP>", 2, 3, TEST_TOKEN, TRI_GRAM_TOKENS_ALT_SEPARATOR,
                           TRI_GRAM_POSITION_INCREMENTS_ALT_SEPARATOR, 
                           TRI_GRAM_TYPES_ALT_SEPARATOR, true);
  }
  
  @Test
  public void testTriGramFilterWithoutUnigramsAltSeparator() throws IOException {
    this.shingleFilterTest("<SEP>", 2, 3, TEST_TOKEN, 
                           TRI_GRAM_TOKENS_WITHOUT_UNIGRAMS_ALT_SEPARATOR,
                           TRI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS_ALT_SEPARATOR,
                           TRI_GRAM_TYPES_WITHOUT_UNIGRAMS_ALT_SEPARATOR, false);
  }

  @Test
  public void testTriGramFilterNullSeparator() throws IOException {
    this.shingleFilterTest(null, 2, 3, TEST_TOKEN, TRI_GRAM_TOKENS_NULL_SEPARATOR,
                           TRI_GRAM_POSITION_INCREMENTS_NULL_SEPARATOR, 
                           TRI_GRAM_TYPES_NULL_SEPARATOR, true);
  }

  @Test
  public void testPositionIncrementEqualToN() throws IOException {
    this.shingleFilterTest(2, 3, TEST_TOKEN_POS_INCR_EQUAL_TO_N, TRI_GRAM_TOKENS_POS_INCR_EQUAL_TO_N,
                           TRI_GRAM_POSITION_INCREMENTS_POS_INCR_EQUAL_TO_N, 
                           TRI_GRAM_TYPES_POS_INCR_EQUAL_TO_N, true);
  }
  
  @Test
  public void testPositionIncrementEqualToNWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, 3, TEST_TOKEN_POS_INCR_EQUAL_TO_N, TRI_GRAM_TOKENS_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS,
                           TRI_GRAM_POSITION_INCREMENTS_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS, 
                           TRI_GRAM_TYPES_POS_INCR_EQUAL_TO_N_WITHOUT_UNIGRAMS, false);
  }
  
  
  @Test
  public void testPositionIncrementGreaterThanN() throws IOException {
    this.shingleFilterTest(2, 3, TEST_TOKEN_POS_INCR_GREATER_THAN_N, TRI_GRAM_TOKENS_POS_INCR_GREATER_THAN_N,
                           TRI_GRAM_POSITION_INCREMENTS_POS_INCR_GREATER_THAN_N, 
                           TRI_GRAM_TYPES_POS_INCR_GREATER_THAN_N, true);
  }
  
  @Test
  public void testPositionIncrementGreaterThanNWithoutUnigrams() throws IOException {
    this.shingleFilterTest(2, 3, TEST_TOKEN_POS_INCR_GREATER_THAN_N, TRI_GRAM_TOKENS_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS,
                           TRI_GRAM_POSITION_INCREMENTS_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS, 
                           TRI_GRAM_TYPES_POS_INCR_GREATER_THAN_N_WITHOUT_UNIGRAMS, false);
  }
  
  @Test
  public void testReset() throws Exception {
    Tokenizer wsTokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, new StringReader("prefix please divide this sentence"));
    TokenStream filter = new PrefixedShingleFilter(wsTokenizer, 2);
    assertTokenStreamContents(filter,
      new String[]{"prefix please","prefix please divide","prefix divide","prefix divide this","prefix this","prefix this sentence","prefix sentence"},
      new int[]{0,0,7,7,14,14,19}, new int[]{6,13,13,18,18,27,27},
      new String[]{TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE},
      new int[]{1,0,1,0,1,0,1}
    );
    wsTokenizer.reset(new StringReader("prefix please divide this sentence"));
    assertTokenStreamContents(filter,
      new String[]{"prefix please","prefix please divide","prefix divide","prefix divide this","prefix this","prefix this sentence","prefix sentence"},
      new int[]{0,0,7,7,14,14,19}, new int[]{6,13,13,18,18,27,27},
      new String[]{TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE,"shingle",TypeAttribute.DEFAULT_TYPE},
      new int[]{1,0,1,0,1,0,1}
    );
  }

  @Test
  public void testOutputUnigramsIfNoShinglesSingleTokenCase() throws IOException {
    // Single token input with outputUnigrams==false is the primary case where
    // enabling this option should alter program behavior.
    this.shingleFilterTest(2, 2, TEST_SINGLE_TOKEN, SINGLE_TOKEN,
                           SINGLE_TOKEN_INCREMENTS, SINGLE_TOKEN_TYPES,
                           false, true);
  }
 
  @Test
  public void testOutputUnigramsIfNoShinglesWithSimpleBigram() throws IOException {
    // Here we expect the same result as with testBiGramFilter().
    this.shingleFilterTest(2, 2, TEST_TOKEN, BI_GRAM_TOKENS,
                           BI_GRAM_POSITION_INCREMENTS, BI_GRAM_TYPES,
                           true, true);
  }

  @Test
  public void testOutputUnigramsIfNoShinglesWithSimpleUnigramlessBigram() throws IOException {
    // Here we expect the same result as with testBiGramFilterWithoutUnigrams().
    this.shingleFilterTest(2, 2, TEST_TOKEN, BI_GRAM_TOKENS_WITHOUT_UNIGRAMS,
                           BI_GRAM_POSITION_INCREMENTS_WITHOUT_UNIGRAMS, BI_GRAM_TYPES_WITHOUT_UNIGRAMS,
                           false, true);
  }

  @Test
  public void testOutputUnigramsIfNoShinglesWithMultipleInputTokens() throws IOException {
    // Test when the minimum shingle size is greater than the number of input tokens
    this.shingleFilterTest(7, 7, TEST_TOKEN, UNI_GRAM_TOKENS, 
                           UNIGRAM_ONLY_POSITION_INCREMENTS, UNIGRAM_ONLY_TYPES,
                           false, true);
  }

  protected void shingleFilterTest(int maxSize, Token[] tokensToShingle, Token[] tokensToCompare,
                                   int[] positionIncrements, String[] types,
                                   boolean outputUnigrams)
    throws IOException {

    PrefixedShingleFilter filter = new PrefixedShingleFilter(new TestTokenStream(tokensToShingle), maxSize);
    filter.setOutputUnigrams(outputUnigrams);
    shingleFilterTestCommon(filter, tokensToCompare, positionIncrements, types);
  }

  protected void shingleFilterTest(int minSize, int maxSize, Token[] tokensToShingle, 
                                   Token[] tokensToCompare, int[] positionIncrements,
                                   String[] types, boolean outputUnigrams)
    throws IOException {
    PrefixedShingleFilter filter 
      = new PrefixedShingleFilter(new TestTokenStream(tokensToShingle), minSize, maxSize);
    filter.setOutputUnigrams(outputUnigrams);
    shingleFilterTestCommon(filter, tokensToCompare, positionIncrements, types);
  }

  protected void shingleFilterTest(int minSize, int maxSize, Token[] tokensToShingle, 
                                   Token[] tokensToCompare, int[] positionIncrements,
                                   String[] types, boolean outputUnigrams, 
                                   boolean outputUnigramsIfNoShingles)
    throws IOException {
    PrefixedShingleFilter filter 
      = new PrefixedShingleFilter(new TestTokenStream(tokensToShingle), minSize, maxSize);
    filter.setOutputUnigrams(outputUnigrams);
    filter.setOutputUnigramsIfNoShingles(outputUnigramsIfNoShingles);
    shingleFilterTestCommon(filter, tokensToCompare, positionIncrements, types);
  }

  protected void shingleFilterTest(String tokenSeparator, int minSize, int maxSize, Token[] tokensToShingle, 
                                   Token[] tokensToCompare, int[] positionIncrements,
                                   String[] types, boolean outputUnigrams)
    throws IOException {
    PrefixedShingleFilter filter 
      = new PrefixedShingleFilter(new TestTokenStream(tokensToShingle), minSize, maxSize);
    filter.setTokenSeparator(tokenSeparator);
    filter.setOutputUnigrams(outputUnigrams);
    shingleFilterTestCommon(filter, tokensToCompare, positionIncrements, types);
  }

  protected void shingleFilterTestCommon(PrefixedShingleFilter filter,
                                         Token[] tokensToCompare,
                                         int[] positionIncrements,
                                         String[] types)
    throws IOException {
    String text[] = new String[tokensToCompare.length];
    int startOffsets[] = new int[tokensToCompare.length];
    int endOffsets[] = new int[tokensToCompare.length];
    
    for (int i = 0; i < tokensToCompare.length; i++) {
      text[i] = new String(tokensToCompare[i].buffer(),0, tokensToCompare[i].length());
      startOffsets[i] = tokensToCompare[i].startOffset();
      endOffsets[i] = tokensToCompare[i].endOffset();
    }
    
    assertTokenStreamContents(filter, text, startOffsets, endOffsets, types, positionIncrements);
  }
  
  private static Token createToken(String term, int start, int offset) {
    return createToken(term, start, offset, 1);
  }

  private static Token createToken
    (String term, int start, int offset, int positionIncrement)
  {
    Token token = new Token(start, offset);
    token.copyBuffer(term.toCharArray(), 0, term.length());
    token.setPositionIncrement(positionIncrement);
    return token;
  }
  
  /** blast some random strings through the analyzer */
  @Test
  public void testRandomStrings() throws Exception {
    Analyzer a = new ReusableAnalyzerBase() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new PrefixedShingleFilter(tokenizer));
      }
    };
    checkRandomData(random, a, 10000*RANDOM_MULTIPLIER);
  }
  
  /** blast some random large strings through the analyzer */
  @Test
  public void testRandomHugeStrings() throws Exception {
    Analyzer a = new ReusableAnalyzerBase() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new PrefixedShingleFilter(tokenizer));
      }
    };
    checkRandomData(random, a, 200*RANDOM_MULTIPLIER, 8192);
  }
  
  @Test
  public void testEmptyTerm() throws IOException {
    Analyzer a = new ReusableAnalyzerBase() {
      @Override
      protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new KeywordTokenizer(reader);
        return new TokenStreamComponents(tokenizer, new PrefixedShingleFilter(tokenizer));
      }
    };
    assertAnalyzesToReuse(a, "", new String[]{});
    //checkOneTermReuse(a, "prefix", "");
  }
}
