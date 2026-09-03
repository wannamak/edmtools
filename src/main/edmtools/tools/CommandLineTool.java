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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.OptionHandlerRegistry;
import org.kohsuke.args4j.spi.DelimitedOptionHandler;
import org.kohsuke.args4j.spi.IntOptionHandler;
import org.kohsuke.args4j.spi.Setter;

/** Tool superclass to parse arguments. */
abstract class CommandLineTool {
  @Option(name = "-h", usage="get help", aliases={"-h", "--help", "--h"})
  private boolean showHelp;

  @Argument
  protected List<String> args = new ArrayList<>();

  public static class MultiIntegerOptionHandler extends DelimitedOptionHandler<Integer> {
    public MultiIntegerOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Integer> setter) {
      super(parser, option, setter, ",", new IntOptionHandler(parser, option, setter));
    }
  }

  public static void initAndRun(String rawArgs[], CommandLineTool instance) throws Exception {
    OptionHandlerRegistry.getRegistry().registerHandler(Integer.class, MultiIntegerOptionHandler.class);
    CmdLineParser parser = new CmdLineParser(instance);
    try {
      parser.parseArgument(rawArgs);
    } catch (CmdLineException e) {
      System.out.println(e);
      parser.printUsage(System.out);
      System.exit(1);
    }
    if (instance.showHelp) {
      parser.printUsage(System.out);
      System.exit(1);
    }
    instance.run();
  }

  public abstract void run() throws Exception;
}
