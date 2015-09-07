/**
 *    Copyright 2015 Keith Wannamaker
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edmtools.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/** Tool superclass to parse arguments and programatically configure java logging. */
abstract class CommandLineTool {
  @Option(name = "-v", usage="verbosity (0-3 are supported)", aliases="--v")
  private int verbosity = 0;

  @Option(name = "-h", usage="get help", aliases={"-h", "--help", "--h"})
  private boolean showHelp;
  
  @Argument
  protected List<String> args = new ArrayList<>();
  
  public static void initAndRun(String rawArgs[], CommandLineTool instance) throws Exception {
    CmdLineParser parser = new CmdLineParser(instance);
    try {
      parser.parseArgument(rawArgs);
      setVerbosity(instance.verbosity);
    } catch (CmdLineException e) {
      parser.printUsage(System.out);
      System.exit(1);
    }
    if (instance.showHelp) {
      parser.printUsage(System.out);
      System.exit(1);
    }
    instance.run();
  }
  
  public static void setVerbosity(int verbosity) {
    Level level = mapVerbosityToLevel(verbosity);
    Logger logger = Logger.getLogger("edmtools");
    logger.setLevel(level);
    Handler handlers[] = Logger.getLogger("").getHandlers();
    if (handlers.length == 0) {
      System.err.println("Unable to set logging level");
    } else {
      handlers[0].setLevel(level);
    }
  }
  
  private static Level mapVerbosityToLevel(int verbosity) {
    switch (verbosity) {
      case 0:  return Level.INFO;
      case 1:  return Level.FINE;
      case 2:  return Level.FINER;
      case 3:  return Level.FINEST;
      default:
        return verbosity > 3 ? Level.FINEST : Level.OFF;
    }
  }
  
  public abstract void run() throws Exception;
}
