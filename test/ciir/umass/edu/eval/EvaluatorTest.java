package ciir.umass.edu.eval;

import ciir.umass.edu.learning.CoorAscent;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.tree.LambdaMART;
import ciir.umass.edu.learning.tree.RFRanker;
import ciir.umass.edu.utilities.FileUtils;
import ciir.umass.edu.utilities.TmpFile;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author jfoley.
 */
public class EvaluatorTest extends RankyTest {
  @Test
  public void testCLINoArgs() {
    // DataPoint has ugly globals, so for now, make this threadsafe
    synchronized (DataPoint.class) {
      Evaluator.main(new String[]{});
    }
  }


  // This is a pretty crazy test;
  //  1. First it cooks up a data file in which feature 1 is good, and feature 2 is random crap.
  //  2. Then it runs the Evaluator to tune for MAP.
  //  3. Then it manually loads the Ranker, checks its the kind we asked for, and then makes sure that it learned something valuable.
  @Test
  public void testCoorAscent() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile()) {

      writeRandomData(dataFile);

      synchronized (DataPoint.class) {
        Evaluator.main(new String[] {
            "-train", dataFile.getPath(),
            "-metric2t", "map",
            "-ranker", "4",
            "-save", modelFile.getPath()});
      }

      RankerFactory rf = new RankerFactory();
      Ranker model = rf.loadRankerFromFile(modelFile.getPath());

      assertTrue(model instanceof CoorAscent);
      CoorAscent cmodel = (CoorAscent) model;
      System.out.println(Arrays.toString(cmodel.weight));

      assertTrue("Computed weight vector doesn't make sense with our fake data: "+Arrays.toString(cmodel.weight), cmodel.weight[0] > cmodel.weight[1]);

      assertTrue("Computed weight vector doesn't make sense with our fake data: "+Arrays.toString(cmodel.weight), cmodel.weight[0] > 0.9);
      assertTrue("Computed weight vector doesn't make sense with our fake data: "+Arrays.toString(cmodel.weight), cmodel.weight[1] < 0.1);
    }
  }

  void writeRandomData(TmpFile dataFile) throws IOException {
    try (PrintWriter out = dataFile.getWriter()) {
      // feature 1 is the only good one:
      Random rand = new Random();
      for (int i = 0; i < 100; i++) {
        String w1 = (rand.nextBoolean() ? "-1.0" : "1.0");
        String w2 = (rand.nextBoolean() ? "-1.0" : "1.0");
        out.println("1 qid:x 1:1.0 2:"+w1+" # P"+Integer.toString(i));
        out.println("0 qid:x 1:0.9 2:"+w2+" # N"+Integer.toString(i));
      }
    }
  }

  void writeRandomDataCount(TmpFile dataFile, int numQ, int numD) throws IOException {
    try (PrintWriter out = dataFile.getWriter()) {
      // feature 1 is the only good one:
      Random rand = new Random();
      for (int q = 0; q < numQ; q++) {
        String qid = Integer.toString(q);
        for (int i = 0; i < numD; i++) {
          String w1 = (rand.nextBoolean() ? "-1.0" : "1.0");
          String w2 = (rand.nextBoolean() ? "-1.0" : "1.0");
          out.println("1 qid:"+qid+" 1:1.0 2:"+w1+" # P"+Integer.toString(i));
          out.println("0 qid:"+qid+" 1:0.9 2:"+w2+" # N"+Integer.toString(i));
        }
      }
    }
  }

  @Test
  public void testRF() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 8, "map");
    }
  }

  @Test
  public void testLinearReg() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 9, "map");
    }
  }

  @Test
  public void testCAscent() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 4, "map");
    }
  }

  @Test
  public void testMART() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 0, "map");
    }
  }
  // Fails with NaN...
  @Ignore
  @Test
  public void testRankBoost() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 1, "map");
    }
  }
  // Fails with NaN
  @Ignore
  @Test
  public void testRankNet() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 2, "map");
    }
  }
  // Fails with Infinity... or fails to learn, depending on measure (map, err)
  @Ignore
  @Test
  public void testAdaRank() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomDataCount(dataFile, 20, 20);
      testRanker(dataFile, modelFile, rankFile, 3, "map");
    }
  }

  // Works sometimes based on initial conditions :(
  @Ignore
  @Test
  public void testLambdaRank() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomDataCount(dataFile, 10, 50);
      testRanker(dataFile, modelFile, rankFile, 5, "map");
    }
  }
  @Test
  public void testLambdaMART() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 6, "map");
    }
  }

  @Test
  public void testRandomForrestParseModel() throws IOException {


    RankerFactory factory = new RankerFactory();

  }

  @Test
  public void testEnsemble() throws IOException {

    String ensembleHeader =  "## Random Forests \n" +
            "## name:foo\n" +
            "## No. of trees = 1\n" +
            "## No. of leaves = 10\n" +
            "## No. of threshold candidates = 256\n" +
            "## Learning rate = 0.1\n" +
            "## Stop early = 100\n" +
            "\n";

    String ensemble = ensembleHeader +
            "<ensemble>\n" +
            " <tree id=\"1\" weight=\"0.1\">\n" +
            "  <split>\n" +
            "   <feature> 1 </feature>\n" +
            "   <threshold> 0.45867884 </threshold>\n" +
            "   <split pos=\"left\">\n" +
            "    <feature> 1 </feature>\n" +
            "    <threshold> 0.0 </threshold>\n" +
            "    <split pos=\"left\">\n" +
            "     <output> -2.0 </output>\n" +
            "    </split>\n" +
            "    <split pos=\"right\">\n" +
            "     <output> -1.3413081169128418 </output>\n" +
            "    </split>\n" +
            "   </split>\n" +
            "   <split pos=\"right\">\n" +
            "    <feature> 1 </feature>\n" +
            "    <threshold> 0.6115718 </threshold>\n" +
            "    <split pos=\"left\">\n" +
            "     <output> 0.3089442849159241 </output>\n" +
            "    </split>\n" +
            "    <split pos=\"right\">\n" +
            "     <output> 2.0 </output>\n" +
            "    </split>\n" +
            "   </split>\n" +
            "  </split>\n" +
            " </tree></ensemble>";


    RankerFactory factory = new RankerFactory();
    Ranker ranker = factory.loadRankerFromString(ensemble);
    RFRanker rfRanker = (RFRanker) ranker;
    assert(rfRanker != null);
    assert(rfRanker.getEnsembles().length == 1);


    String ensemble2 = ensembleHeader + "\n" +
            "<ensemble>" +
            " <tree id=\"1\" weight=\"0.1\">" +
            "  <split>" +
            "   <feature> 1 </feature>" +
            "   <threshold> 0.45867884 </threshold>" +
            "   <split pos=\"left\">" +
            "    <feature> 1 </feature>" +
            "    <threshold> 0.0 </threshold>" +
            "    <split pos=\"left\">" +
            "     <output> -2.0 </output>" +
            "    </split>" +
            "    <split pos=\"right\">" +
            "     <output> -1.3413081169128418 </output>" +
            "    </split>" +
            "   </split>" +
            "   <split pos=\"right\">" +
            "    <feature> 1 </feature>" +
            "    <threshold> 0.6115718 </threshold>" +
            "    <split pos=\"left\">" +
            "     <output> 0.3089442849159241 </output>" +
            "    </split>" +
            "    <split pos=\"right\">" +
            "     <output> 2.0 </output>" +
            "    </split>" +
            "   </split>" +
            "  </split>" +
            " </tree></ensemble>";

//    ranker = factory.loadRankerFromString(ensemble2.replace(">"));
//    rfRanker = (RFRanker) ranker;
//    assert(rfRanker != null);
//    assert(rfRanker.getEnsembles().length == 2);


  }

  @Test
  public void testLambdaMARTParseModel() throws IOException {

    String simpleModel = "## LambdaMART\n" +
            "## name:foo\n" +
            "## No. of trees = 1\n" +
            "## No. of leaves = 10\n" +
            "## No. of threshold candidates = 256\n" +
            "## Learning rate = 0.1\n" +
            "## Stop early = 100\n" +
            "\n" +
            "<ensemble>\n" +
            " <tree id=\"1\" weight=\"0.1\">\n" +
            "  <split>\n" +
            "   <feature> 1 </feature>\n" +
            "   <threshold> 0.45867884 </threshold>\n" +
            "   <split pos=\"left\">\n" +
            "    <feature> 1 </feature>\n" +
            "    <threshold> 0.0 </threshold>\n" +
            "    <split pos=\"left\">\n" +
            "     <output> -2.0 </output>\n" +
            "    </split>\n" +
            "    <split pos=\"right\">\n" +
            "     <output> -1.3413081169128418 </output>\n" +
            "    </split>\n" +
            "   </split>\n" +
            "   <split pos=\"right\">\n" +
            "    <feature> 1 </feature>\n" +
            "    <threshold> 0.6115718 </threshold>\n" +
            "    <split pos=\"left\">\n" +
            "     <output> 0.3089442849159241 </output>\n" +
            "    </split>\n" +
            "    <split pos=\"right\">\n" +
            "     <output> 2.0 </output>\n" +
            "    </split>\n" +
            "   </split>\n" +
            "  </split>\n" +
            " </tree>\n" +
            "</ensemble>";

      RankerFactory factory = new RankerFactory();
      Ranker ranker = factory.loadRankerFromString(simpleModel);
      LambdaMART lambdaMART = (LambdaMART)ranker;
      assert(lambdaMART != null);
  }

  @Test
  public void testListNet() throws IOException {
    try (TmpFile dataFile = new TmpFile();
         TmpFile modelFile = new TmpFile();
         TmpFile rankFile = new TmpFile()
    ) {
      writeRandomData(dataFile);
      testRanker(dataFile, modelFile, rankFile, 6, "map");
    }
  }
}
