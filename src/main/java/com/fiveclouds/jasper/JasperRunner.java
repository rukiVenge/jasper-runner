/**
 Copyright (c) 2011 Philip Dodds

 Permission is hereby granted, free of charge, to any person
 obtaining a copy of this software and associated documentation
 files (the "Software"), to deal in the Software without
 restriction, including without limitation the rights to use,
 copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the
 Software is furnished to do so, subject to the following
 conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 OTHER DEALINGS IN THE SOFTWARE.
 */
package com.fiveclouds.jasper;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import org.apache.commons.cli.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A simple helper which can be used to export a PDF of a Jasper Report
 * <p/>
 * Note this is a very simple little utility
 */
public class JasperRunner {

    public static void main(String[] args) {

        // Set-up the options for the utility
        Options options = new Options();
        Option report = new Option("report", true, "jasper report to run (i.e. /path/to/report.jrxml)");
        options.addOption(report);

        Option driver = new Option("driver", true, "the jdbc driver class (i.e. com.mysql.jdbc.Driver)");
        driver.setRequired(true);
        options.addOption(driver);

        options.addOption("jdbcurl", true, "database jdbc url (i.e. jdbc:mysql://localhost:3306/database)");
        options.addOption("excel", true, "Will override the PDF default and export to Microsoft Excel");
        options.addOption("username", true, "database username");
        options.addOption("password", true, "database password");
        options.addOption("output", true, "the output filename (i.e. path/to/report.pdf");
        options.addOption("help", false, "print this message");

        Option propertyOption = OptionBuilder.withArgName("property=value")
                .hasArgs(2)
                .withValueSeparator()
                .withDescription("use value as report property")
                .create("D");

        options.addOption(propertyOption);

        // Parse the options and build the report
        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("jasper-runner", options);
            } else {

                System.out.println("Building report " + cmd.getOptionValue("report"));
                try {
                    Class.forName(cmd.getOptionValue("driver"));
                    Connection connection = DriverManager.getConnection(cmd.getOptionValue("jdbcurl"), cmd.getOptionValue("username"), cmd.getOptionValue("password"));
                    System.out.println("Connected to " + cmd.getOptionValue("jdbcurl"));
                    JasperReport jasperReport = JasperCompileManager.compileReport(cmd.getOptionValue("report"));

                    JRPdfExporter pdfExporter = new JRPdfExporter();

                    Properties properties = cmd.getOptionProperties("D");
                    Map<String, Object> parameters = new HashMap<String, Object>();

                    Map<String, JRParameter> reportParameters = new HashMap<String, JRParameter>();

                    for (JRParameter param : jasperReport.getParameters()) {
                        reportParameters.put(param.getName(), param);
                    }

                    for (Object propertyKey : properties.keySet()) {
                        String parameterName = String.valueOf(propertyKey);
                        String parameterValue = String.valueOf(properties.get(propertyKey));
                        JRParameter reportParam = reportParameters.get(parameterName);

                        if (reportParam != null) {
                            if (reportParam.getValueClass().equals(String.class)) {
                                System.out.println("Property " + parameterName + " set to String = " + parameterValue);
                                parameters.put(parameterName, parameterValue);
                            } else if (reportParam.getValueClass().equals(Integer.class)) {
                                System.out.println("Property " + parameterName + " set to Integer = " + parameterValue);
                                parameters.put(parameterName, Integer.parseInt(parameterValue));
                            } else {
                                System.err.print("Unsupported type for property "+parameterName);
                                System.exit(1);

                            }
                        } else {
                            System.out.println("Property " + parameterName + " not found in the report! IGNORING");

                        }
                    }

                    JasperPrint print = JasperFillManager.fillReport(jasperReport, parameters, connection);

                    pdfExporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
                    pdfExporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, cmd.getOptionValue("output"));
                    System.out.println("Exporting report to " + cmd.getOptionValue("output"));
                    pdfExporter.exportReport();
                } catch (JRException e) {
                    System.err.print("Unable to parse report file (" + cmd.getOptionValue("r") + ")");
                    e.printStackTrace();
                    System.exit(1);
                } catch (ClassNotFoundException e) {
                    System.err.print("Unable to find the database driver,  is it on the classpath?");
                    e.printStackTrace();
                    System.exit(1);
                } catch (SQLException e) {
                    System.err.print("An SQL exception has occurred (" + e.getMessage() + ")");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } catch (ParseException e) {
            System.err.print("Unable to parse command line options (" + e.getMessage() + ")");
            System.exit(1);
        }
    }
}
