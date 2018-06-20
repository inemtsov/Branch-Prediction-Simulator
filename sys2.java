/*
  Ilya Nemtsov

  Instructions how to compile and run
    1. to compile the sys2:
      a. navigate in terminal to the directory where sys2.java file is located
      b. type in terminal: javac sys2.java
      c. the previous command (b) will create sys2.class file

    2. to run the sys2:
      a. type in terminal: java sys2 <path to trace file: gcc.xac or sjeng.xac> <N> <M> [-v]
        i. [-v] is optional argument to enable a verbose mode
        j. required arguments: N (number of entries in branch predictor) and M (number of entries in branch target buffer)

     example:
      javac sys2.java
      java sys2 ~whsu/csc656/Traces/S18/P1/gcc.xac 128 256 -v
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class sys2 {

  private boolean isVerboseModeOn;
  private BufferedReader source;
  private String line;
  private int numConditionalBranches, numForwardBranches, numBackwardBranches, numForwardTakenBranches, numBackwardTakenBranches, numMispredictedBranches;
  private int N, M;
  private int[][] branchTargetBuffer;
  private int[] predictionBuffer;
  private int validStateIndex = 0, BTAIndex = 1, tagIndex = 2;
  private int orderOfBranch = 0;
  private int numOfBTBmisses = 0;
  private int numOfBTBaccesses = 0;

  public sys2( String fileName, String N, String M, boolean isVerboseModeOn ) {
    this.isVerboseModeOn = isVerboseModeOn;
    this.N = Integer.parseInt( N );
    this.M = Integer.parseInt( M );
    try {
      //initialize prediction buffer and branch target buffer
      initPredictionBuffer( this.N );
      initBranchTargetBuffer( this.M );

      //open trace file
      source = new BufferedReader( new FileReader( fileName ) );
      load();

      //print the measurements
      printOutput();
    }catch( IOException e ) {
      source = null;
    }
  }

  //initialize prediction and assign all predictors states to 01
  private void initPredictionBuffer( int num ) {
    predictionBuffer = new int[num];
    for( int index = 0; index < num; index++ ) {
      predictionBuffer[index] = 1;
    }
  }

  //initialize branch target buffer and assign all valid bits to 0
  private void initBranchTargetBuffer( int num ) {
    branchTargetBuffer = new int[num][3];
    for( int index = 0; index < num; index++ ) {
      branchTargetBuffer[index][validStateIndex] = 0;
    }
  }

  //parsing each line of the file and extracting info for measurements
  private void parseLine( String currentLine ) {
    //splits the line based on spaces
    String[] tokens = currentLine.split( "\\s+" );

    if( tokens[6].equals( "T" ) || tokens[6].equals( "N" ) ) {

      //get index of prediction buffer
      int indexOfPredictor = calculateIndex( tokens[1], N );

      //find the current state of prediction buffer
      int currStateOfPredictor = getCurrStateOfPredictionBuffer( indexOfPredictor );

      //get index of branch target buffer
      int indexOfBTB = calculateIndex( tokens[1], M );

      //get tag of a current branch
      int currTag = getTagOfBranch( tokens[1], M );
      //System.out.println(currTag);
      //number of conditional branches
      numConditionalBranches++;

      //checks if branch is taken or not, and also if it's backward or forward
      //https://stackoverflow.com/questions/20110533/converting-hexadecimal-string-to-decimal-integer
      if( (Integer.parseInt( tokens[1], 16 ) - Integer.parseInt( tokens[11], 16 )) < 0 ) {

        numForwardBranches++;
        if( tokens[6].equals( "T" ) ) {
          numForwardTakenBranches++;
        }
      }else {

        numBackwardBranches++;
        if( tokens[6].equals( "T" ) ) {
          numBackwardTakenBranches++;
        }
      }

      //check if predictor says branch Taken
      if( currStateOfPredictor == 2 || currStateOfPredictor == 3 ) {
        //update # of BTB accesses and misses
        numOfBTBaccesses++;
        if( getValidBit( indexOfBTB ) != 1 || getTagOfBTB( indexOfBTB ) != currTag ) {
          numOfBTBmisses++;
        }
      }

      //check whether prediction says branch is taken and actual behavior is the same
      if( tokens[6].equals( "T" ) && (currStateOfPredictor != 2 && currStateOfPredictor != 3) ) {
        numMispredictedBranches++;
      }

      if( tokens[6].equals( "N" ) && (currStateOfPredictor != 0 && currStateOfPredictor != 1) ) {
        numMispredictedBranches++;
      }

      setNewPrediction( currStateOfPredictor, indexOfPredictor, tokens[6] );

      int newStateOfPredictor = predictionBuffer[indexOfPredictor];

      if( tokens[6].equals( "T" ) ) {
        setBTBInfo( indexOfBTB, 1, tokens[11], currTag );
      }

      //if verbose mode is on, prints information needed for debugging
      if( isVerboseModeOn ) {
//        System.out.println( orderOfBranch++ + "      " + Integer.toHexString( indexOfPredictor ) + "      " + currStateOfPredictor + "      " +
//                +newStateOfPredictor + "       " + Integer.toHexString( indexOfBTB ) + "      " + Integer.toHexString( currTag ) + "      " +
//                +numOfBTBaccesses + "      " + numOfBTBmisses );

        System.out.printf( "%-10s %-5s %-5s %-5s %-5s %-10s %-10s %-10s", orderOfBranch++, Integer.toHexString( indexOfPredictor ), currStateOfPredictor, newStateOfPredictor,
                Integer.toHexString( indexOfBTB ), Integer.toHexString( currTag ), numOfBTBaccesses, numOfBTBmisses );
        System.out.println();
      }
    }
  }

  private void load() {
    while( hasMoreLines() ) {
      parseLine( getLine() );
    }
  }

  //checks if there are more lines to parse
  private boolean hasMoreLines() {
    line = nextLine();
    return line == null ? false : true;
  }

  //read the next line
  private String nextLine() {
    try {
      return source.readLine();
    }catch( Exception e ) {
      return null;
    }
  }

  private String getLine() {
    return line;
  }

  //calculate the index for predictor buffer and branch target buffer
  private int calculateIndex( String address, int num ) {
    String binNumber = "0" + Integer.toBinaryString( Integer.parseInt( address, 16 ) );
    int logNbits = ( int ) (Math.log( num ) / Math.log( 2 ));
    return Integer.parseInt( binNumber.substring( binNumber.length() - logNbits ), 2 );
  }

  //return tag of btb in specified index
  private int getTagOfBTB( int index ) {
    return branchTargetBuffer[index][tagIndex];
  }

  //return a valid bit of btb in specified index
  private int getValidBit( int index ) {
    return branchTargetBuffer[index][validStateIndex];
  }

  //return a calculated tag for specified branch address
  private int getTagOfBranch( String address, int num ) {
    String binNumber = "0" + Integer.toBinaryString( Integer.parseInt( address, 16 ) );
    int logNbits = ( int ) (Math.log( num ) / Math.log( 2 ));
    //System.out.println( binNumber.substring( 0, binNumber.length() - logNbits ) );
    return Integer.parseInt( binNumber.substring( 0, binNumber.length() - logNbits ), 2 );
  }

  //return a current state of prediction buffer
  private int getCurrStateOfPredictionBuffer( int index ) {
    return predictionBuffer[index];
  }

  //update prediction buffer
  private void setNewPrediction( int state, int index, String ch ) {
    char newChar = ch.charAt( 0 );
    if( newChar == 'T' && state != 3 ) {
      predictionBuffer[index] = ++state;
    }else if( newChar == 'N' && state != 0 ) {
      predictionBuffer[index] = --state;
    }
    // System.out.println(state + " " +  index +" " + ch + " " + predictionBuffer[index]);
  }

  //update BTB info
  private void setBTBInfo( int index, int bit, String targetAddress, int tag ) {
    branchTargetBuffer[index][validStateIndex] = bit;
    branchTargetBuffer[index][BTAIndex] = Integer.parseInt( targetAddress, 16 ); //store target address in BTA as integer
    branchTargetBuffer[index][tagIndex] = tag;
    //System.out.println( branchTargetBuffer[index][validStateIndex] + " " + branchTargetBuffer[index][BTAIndex] + " " + Integer.toHexString(branchTargetBuffer[index][tagIndex]) );
  }

  //print the measurements
  private void printOutput() {

    System.out.println( "The total number of conditional branches: " + numConditionalBranches );
    System.out.println( "The number of forward branches: " + numForwardBranches );
    System.out.println( "The number of backward branches: " + numBackwardBranches );
    System.out.println( "The number of forward taken branches: " + numForwardTakenBranches );
    System.out.println( "The number of backward taken branches: " + numBackwardTakenBranches );
    System.out.println( "The number of mispredicted branches: " + numMispredictedBranches );
    System.out.println( "The misprediction rate for all branches: " + ( float ) numMispredictedBranches / ( float ) numConditionalBranches );
    System.out.println( "The number of BTB misses: " + numOfBTBmisses );
    System.out.println( "The BTB miss rate: " + ( float ) numOfBTBmisses / ( float ) numOfBTBaccesses );

  }

  public static void main( String args[] ) {
    int lengthOfArg = args.length;
    boolean isVerbose = false;

    //checks the arguments
    if( lengthOfArg > 3 ) {
      if( !args[3].equals( "-v" ) || lengthOfArg != 4 ) {

        System.out.println( "The fourth argument is not correct or too many arguments (more than 4)." );
      }else {
        isVerbose = true;
        if( checkArgument( args[1] ) && checkArgument( args[2] ) ) {
          new sys2( args[0], args[1], args[2], isVerbose );
        }else {
          System.out.println( "Number of entries in predictor buffer or branch target buffer must be positive a power of two." );
        }
      }
    }else if( lengthOfArg < 3 ) {
      System.out.println( "Please provide arguments(1: file name \n" +
              "                                     2: number of entries in predict buffer \n" +
              "                                     3: number of entries in branch target buffer \n" +
              "                                     4(Optional):\"-v\"" );
    }else {
      if( checkArgument( args[1] ) && checkArgument( args[2] ) ) {
        new sys2( args[0], args[1], args[2], isVerbose );
      }else {
        System.out.println( "The number of entries in predictor buffer or branch target buffer must be positive and a power of two." );
      }
    }
  }

  private static boolean checkArgument( String num ) {
    int number = Integer.parseInt( num );
    return ((number > 0) && (number & (number - 1)) == 0);
  }

}
