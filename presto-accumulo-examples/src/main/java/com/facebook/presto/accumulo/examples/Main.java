/**
 * Copyright 2016 Bloomberg L.P.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.accumulo.examples;

import com.facebook.presto.accumulo.conf.AccumuloConfig;
import com.facebook.presto.accumulo.tools.Task;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.reflections.Reflections;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.String.format;

public class Main
        extends Configured
        implements Tool
{
    /**
     * List of all discovered tasks
     */
    private static List<Task> tasks = new ArrayList<>();

    private static final Option HELP =
            OptionBuilder.withDescription("Print this help message").withLongOpt("help").create();

    static {
        // Search the classpath for implementations of Task in the examples package
        Reflections reflections = new Reflections("com.facebook.presto.accumulo.examples");
        for (Class<? extends Task> task : reflections.getSubTypesOf(Task.class)) {
            try {
                tasks.add(task.newInstance());
            }
            catch (InstantiationException | IllegalAccessException e) {
                System.err.println("Failed to instantiate " + task);
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a {@link Task} that matches the given task name
     *
     * @param name Task name to find
     * @return The Task, or null if not found
     */
    public static Task getTask(String name)
    {
        for (Task t : tasks) {
            if (t.getTaskName().equals(name)) {
                return t;
            }
        }

        return null;
    }

    /**
     * Gets all available {@link Task} objects, sorted by name
     *
     * @return Sorted list of Tasks
     */
    public static List<Task> getTasks()
    {
        // Sort by name
        Collections.sort(tasks, new Comparator<Task>()
        {
            @Override
            public int compare(Task o1, Task o2)
            {
                return o1.getTaskName().compareTo(o2.getTaskName());
            }
        });

        return tasks;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int run(String[] args)
            throws Exception
    {
        // If no arguments, print help
        if (args.length == 0) {
            printTools();
            return 1;
        }

        // Validate PRESTO_HOME is set to pull accumulo properties from config path
        String prestoHome = System.getenv("PRESTO_HOME");
        if (prestoHome == null) {
            System.err.println("PRESTO_HOME is not set.  This is required to locate the "
                    + "etc/catalog/accumulo.properties file");
            System.exit(1);
        }

        // Create an AccumuloConfig from the accumulo properties file
        AccumuloConfig config = com.facebook.presto.accumulo.tools.Main.fromFile(
                new File(System.getenv("PRESTO_HOME"), "etc/catalog/accumulo.properties"));

        // Get the tool name from the first argument
        String toolName = args[0];
        Task t = Main.getTask(toolName);
        if (t == null) {
            System.err.println(format("Unknown tool %s", toolName));
            printTools();
            return 1;
        }

        // Add the help option and all options for the tool
        Options opts = new Options();
        opts.addOption(HELP);
        for (Option o : (Collection<Option>) t.getOptions().getOptions()) {
            opts.addOption(o);
        }

        // Parse command lines
        CommandLine cmd;
        try {
            cmd = new GnuParser().parse(opts, Arrays.copyOfRange(args, 1, args.length));
        }
        catch (ParseException e) {
            printHelp(t);
            return 1;
        }

        // Print help if the option is set
        if (cmd.hasOption(HELP.getLongOpt())) {
            printHelp(t);
            return 0;
        }
        else {
            // Run the tool and print help if anything bad happens
            int code = t.run(config, cmd);
            if (code != 0) {
                printHelp(t);
            }
            return code;
        }
    }

    /**
     * Prints all tools and brief descriptions.
     */
    private void printTools()
    {
        System.out.println("Usage: java -jar <jarfile> <tool> [args]");
        System.out.println(
                "Execute java -jar <jarfile> <tool> --help to see help for a tool.\nAvailable tools:");
        for (Task t : getTasks()) {
            System.out.println("\t" + t.getTaskName() + "\t" + t.getDescription());
        }
    }

    /**
     * Prints the help for a given {@link Task}
     *
     * @param t Task to print help
     */
    @SuppressWarnings("unchecked")
    private void printHelp(Task t)
    {
        Options opts = new Options();
        opts.addOption(HELP);
        for (Option o : (Collection<Option>) t.getOptions().getOptions()) {
            opts.addOption(o);
        }

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(format("usage: java -jar <jarfile> %s [args]", t.getTaskName()), opts);
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new Configuration(), new Main(), args);
    }
}
