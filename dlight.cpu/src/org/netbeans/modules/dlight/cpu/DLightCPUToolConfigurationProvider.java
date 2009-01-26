/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.dlight.cpu;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.netbeans.modules.dlight.api.indicator.IndicatorMetadata;
import org.netbeans.modules.dlight.api.storage.DataRow;
import org.netbeans.modules.dlight.api.storage.DataTableMetadata;
import org.netbeans.modules.dlight.api.storage.DataTableMetadata.Column;
import org.netbeans.modules.dlight.api.tool.DLightToolConfiguration;
import org.netbeans.modules.dlight.collector.stdout.api.CLIODCConfiguration;
import org.netbeans.modules.dlight.collector.stdout.api.CLIOParser;
import org.netbeans.modules.dlight.core.stack.model.FunctionMetric;
import org.netbeans.modules.dlight.core.stack.storage.SQLStackStorage;
import org.netbeans.modules.dlight.core.stack.storage.StackDataStorage;
import org.netbeans.modules.dlight.cpu.impl.CpuIndicatorConfiguration;
import org.netbeans.modules.dlight.dtrace.collector.DTDCConfiguration;
import org.netbeans.modules.dlight.dtrace.collector.MultipleDTDCConfiguration;
import org.netbeans.modules.dlight.perfan.SunStudioDCConfiguration;
import org.netbeans.modules.dlight.spi.tool.DLightToolConfigurationProvider;
import org.netbeans.modules.dlight.util.Util;
import org.netbeans.modules.dlight.visualizers.api.CallersCalleesVisualizerConfiguration;


/**
 *
 * @author mt154047
 */
public final class DLightCPUToolConfigurationProvider implements DLightToolConfigurationProvider {

  private static final boolean USE_DTRACE = Boolean.getBoolean("gizmo.cpu.dtrace");

  public DLightToolConfiguration create() {
    final String toolName = "CPU Monitor";
    final DLightToolConfiguration toolConfiguration = new DLightToolConfiguration(toolName);
    List<Column> indicatorColumns = Arrays.asList(
            new Column("utime", Float.class, "User Time", null),
            new Column("stime", Float.class, "System Time", null),
            new Column("wtime", Float.class, "Wait Time", null));

    final DataTableMetadata dbTableMetadata = new DataTableMetadata("prstat", indicatorColumns);
    CLIODCConfiguration clioCollectorConfiguration = new CLIODCConfiguration("/bin/prstat", "-mv -p @PID -c 1", new MyCLIOParser(), Arrays.asList(dbTableMetadata));
    toolConfiguration.addIndicatorDataProviderConfiguration(clioCollectorConfiguration);

    IndicatorMetadata indicatorMetadata = new IndicatorMetadata(indicatorColumns);
    HashMap<String, Object> indConfiguration = new HashMap<String, Object>();
    indConfiguration.put("aggregation", "avrg");
    DataTableMetadata functionsListMetaData = null;
    if (USE_DTRACE) {
      String scriptFile = Util.copyResource(getClass(), Util.getBasePath(getClass()) + "/resources/calls.d");
      DataTableMetadata profilerTableMetadata = createProfilerTableMetadata();
      DTDCConfiguration dtraceDataCollectorConfiguration = new DTDCConfiguration(scriptFile, Arrays.asList(profilerTableMetadata));
      dtraceDataCollectorConfiguration.setStackSupportEnabled(true);
      toolConfiguration.addDataCollectorConfiguration(new MultipleDTDCConfiguration(dtraceDataCollectorConfiguration, "cpu:"));
      functionsListMetaData = createFunctionsListMetadata(profilerTableMetadata);
    } else {
      SunStudioDCConfiguration sunStudioConfiguration = new SunStudioDCConfiguration(Arrays.asList(SunStudioDCConfiguration.CollectedInfo.FUNCTIONS_LIST));
      toolConfiguration.addDataCollectorConfiguration(sunStudioConfiguration);
      functionsListMetaData = SunStudioDCConfiguration.getDataTableMetaDataFor(Arrays.asList(SunStudioDCConfiguration.CollectedInfo.FUNCTIONS_LIST));
    }
    final DataTableMetadata data = functionsListMetaData;
    CpuIndicatorConfiguration indicatorConfiguration = new CpuIndicatorConfiguration(indicatorMetadata);
    indicatorConfiguration.setVisualizerConfiguration(new CallersCalleesVisualizerConfiguration(data, SunStudioDCConfiguration.getFunctionNameColumnName(), true));
    toolConfiguration.addIndicatorConfiguration(indicatorConfiguration);
    return toolConfiguration;

  }

  private DataTableMetadata createProfilerTableMetadata() {
    Column cpuId = new Column("cpu_id", Integer.class, "CPU", null);
    Column threadId = new Column("thread_id", Integer.class, "Thread", null);
    Column timestamp = new Column("time_stamp", Long.class, "Timestamp", null);
    Column stackId = new Column("leaf_id", Integer.class, "Stack", null);
    return new DataTableMetadata("CallStack", Arrays.asList(cpuId, threadId, timestamp, stackId));
  }

  private DataTableMetadata createFunctionsListMetadata(DataTableMetadata profilerTableMetadata) {
    List<Column> columns = new ArrayList<Column>();
    columns.add(new Column("name", String.class, "Function Name", null));
    List<FunctionMetric> metricsList = SQLStackStorage.METRICS;
    for (FunctionMetric metric : metricsList) {
      columns.add(new Column(metric.getMetricID(), metric.getMetricValueClass(), metric.getMetricDisplayedName(), null));
    }
    DataTableMetadata result = new DataTableMetadata(StackDataStorage.STACK_METADATA_VIEW_NAME, columns, null, Arrays.asList(profilerTableMetadata));
    return result;
  }

  private static class MyCLIOParser implements CLIOParser {

    private final List<String> colnames = Arrays.asList(new String[]{
              "utime",
              "stime",
              "wtime"
            });
    Float utime, stime, wtime;

    public DataRow process(String line) {
      if (line == null) {
        return null;
      }
      String l = line.trim();
      l = l.replaceAll(",", ".");
      String[] tokens = l.split("[ \t]+");

      if (tokens.length != 15) {
        return null;
      }

      try {
        utime = Float.valueOf(tokens[2]);
        stime = Float.valueOf(tokens[3]);
        wtime = Float.valueOf(tokens[8]);
      } catch (NumberFormatException ex) {
        return null;
      }

      return new DataRow(colnames, Arrays.asList(new Float[]{utime, stime, wtime}));
    }
  }
}
