/*
  Ilya Nemtsov

  Instructions how to compile and run
    1. to compile the sys1:
      a. navigate in terminal to the directory where sys1.java file is located
      b. type in terminal: javac sys1.java
      c. the previous command (b) will create sys1.class file

    2. to run the sys1:
      a. type in terminal: java sys1 <path to trace file: gcc.xac or sjeng.xac> [-v]
        i. [-v] is optional argument to enable a verbose mode

     example:
      javac sys1.java
      java sys1 ~whsu/csc656/Traces/S18/P1/gcc.xac -v
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class sys1 {

  private boolean isVerboseModeOn;
  private BufferedReader source;
  private String line;
  private int numConditionalBranches, numForwardBranches, numBackwardBranches, numForwardTakenBranches, numBackwardTakenBranches, numMispredictedBranches;

  //constructor that initialize the system one
  public sys1( String fileName, boolean isVerboseModeOn ) {
    this.isVerboseModeOn = isVerboseModeOn;

    try {
      //open trace file
      source = new BufferedReader( new FileReader( fileName ) );
      load();

      //print the measurements
      printOutput();
    }catch( IOException e ) {
      source = null;
    }
  }

  //parse the line and extracts needed information for measurements
  private void parseLine( String currentLine ) {

    //splits the line based on spaces
    String[] tokens = currentLine.split( "\\s+" );

    //selects the line that contains branch
    if( tokens[6].equals( "T" ) || tokens[6].equals( "N" ) ) {

      //if verbose mode is on, prints information needed for debugging
      if( isVerboseModeOn ) {
        int isTaken = 0;
        if( tokens[6].equals( "T" ) ) {
          isTaken = 1;
        }
        System.out.println( tokens[1] + " " + tokens[11] + " " + isTaken );
      }
      numConditionalBranches++;

      //checks if branch is taken or not, and also if it's backward or forward
      //https://stackoverflow.com/questions/20110533/converting-hexadecimal-string-to-decimal-integer
      if( (Integer.parseInt( tokens[1], 16 ) - Integer.parseInt( tokens[11], 16 )) < 0 ) {

        numForwardBranches++;
        if( tokens[6].equals( "T" ) ) {
          numForwardTakenBranches++;
          numMispredictedBranches++;
        }
      }else {
        numBackwardBranches++;
        if( tokens[6].equals( "T" ) ) {
          numBackwardTakenBranches++;
        }else {
          numMispredictedBranches++;
        }
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

  //reads the next line
  private String nextLine() {
    try {
      return source.readLine();
    }catch( Exception e ) {
      return null;
    }
  }

  //return the current line
  private String getLine() {
    return line;
  }

  //print the output
  private void printOutput() {

    System.out.println( "The total number of conditional branches: " + numConditionalBranches );
    System.out.println( "The number of forward branches: " + numForwardBranches );
    System.out.println( "The number of backward branches: " + numBackwardBranches );
    System.out.println( "The number of forward taken branches: " + numForwardTakenBranches );
    System.out.println( "The number of backward taken branches: " + numBackwardTakenBranches );
    System.out.println( "The number of mispredicted branches: " + numMispredictedBranches );
    System.out.println( "The misprediction rate for all branches: " + ( float ) numMispredictedBranches / ( float ) numConditionalBranches );
  }

  //the main method
  public static void main( String args[] ) {
    int lengthOfArg = args.length;
    boolean isVerbose = false;

    //check the length of arguments
    if( lengthOfArg > 1 ) {
      if( !args[1].equals( "-v" ) || lengthOfArg != 2 ) {
        System.out.println( "The second argument is not correct or too many arguments (more than 2)." );
      }else {
        isVerbose = true;
        new sys1( args[0], isVerbose );
      }
    }else if( lengthOfArg < 1 ) {
      System.out.println( "Please provide arguments(1: file name and 2(Optional):\"-v\"" );
    }else {
      new sys1( args[0], isVerbose );
    }
  }
}
