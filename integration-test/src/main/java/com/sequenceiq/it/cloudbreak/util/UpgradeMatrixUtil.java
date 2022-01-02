package com.sequenceiq.it.cloudbreak.util;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class UpgradeMatrixUtil {

    private UpgradeMatrixUtil() {
        //not called
    }

    private static UpgradeMatrixDefinition getUpgradeMatrixDefinition() throws IOException {
        URL url = UpgradeMatrixUtil.class.getClassLoader().getResource("upgrade/upgrade-matrix-definition.json");
        String upgradeMatrixJson = FileReaderUtils.readFileFromPath(Path.of(url.getPath()));
        return JsonUtil.readValue(upgradeMatrixJson, UpgradeMatrixDefinition.class);
    }

    public static UpgradeVersionsContainer getRandomRuntimeVersions() {
        String targetRuntimeVersion = "";
        String currentRuntimeVersion = "";
        Set<RuntimeUpgradeMatrix> rums = null;
        try {
            rums = getUpgradeMatrixDefinition().getRuntimeUpgradeMatrix();
        } catch (IOException e) {
            throw new RuntimeException("Upgrade matrix json cannot be read. Error was:" + e.getMessage());
        }
        RuntimeUpgradeMatrix rum = (RuntimeUpgradeMatrix) getRandomFromSet(Collections.singleton(rums));
        Runtime targetRuntime = rum.getTargetRuntime();
        targetRuntimeVersion = targetRuntime.getVersion().replace("\\", "");

        Set<Runtime> rts = rum.getSourceRuntime();
        Runtime sourceRuntime = (Runtime) getRandomFromSet(Collections.singleton(rts));
        currentRuntimeVersion = sourceRuntime.getVersion().replace("\\", "");
        return new UpgradeVersionsContainer(currentRuntimeVersion, targetRuntimeVersion);
    }

    private static Object getRandomFromSet(Set<Object> sset) {
        Set<Object> set = (Set<Object>) sset.iterator().next();
        int size = set.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for (Object entry : set) {
            if (i == item) {
                return entry;
            }
            i++;
        }
        return null;
    }

    public static class UpgradeMatrixDefinition {

        private final Set<RuntimeUpgradeMatrix> runtimeUpgradeMatrix;

        @JsonCreator
        public UpgradeMatrixDefinition(@JsonProperty("runtime_upgrade_matrix") Set<RuntimeUpgradeMatrix> runtimeUpgradeMatrix) {
            this.runtimeUpgradeMatrix = runtimeUpgradeMatrix;
        }

        public Set<RuntimeUpgradeMatrix> getRuntimeUpgradeMatrix() {
            return runtimeUpgradeMatrix;
        }
    }

    public static class RuntimeUpgradeMatrix {

        private final Runtime targetRuntime;

        private final Set<Runtime> sourceRuntime;

        @JsonCreator
        public RuntimeUpgradeMatrix(
                @JsonProperty("target_runtime") Runtime targetRuntime,
                @JsonProperty("source_runtime") Set<Runtime> sourceRuntime) {
            this.targetRuntime = targetRuntime;
            this.sourceRuntime = sourceRuntime;
        }

        public Runtime getTargetRuntime() {
            return targetRuntime;
        }

        public Set<Runtime> getSourceRuntime() {
            Set<Runtime> allSourceRuntime = new HashSet<Runtime>();
            for (Runtime rt : sourceRuntime) {
                if (rt.getVersion().contains("|")) {
                    String baseVersion = rt.getVersion().split("\\(")[0];
                    String minorVersions = rt.getVersion().split("\\(")[1];
                    Pattern p = Pattern.compile("(\\d+)+");
                    Matcher m = p.matcher(minorVersions);
                    while (m.find()) {
                        allSourceRuntime.add(new Runtime(baseVersion + m.group()));
                    }
                } else {
                    allSourceRuntime.add(rt);
                }
            }
            return allSourceRuntime;
        }
    }

    public static class UpgradeVersionsContainer {

        private String currentVersion;

        private String targetVersion;

        public UpgradeVersionsContainer(String current, String target) {
            currentVersion = current;
            targetVersion = target;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getTargetVersion() {
            return targetVersion;
        }
    }

    public static class Runtime {

        private final String version;

        @JsonCreator
        public Runtime(@JsonProperty("version") String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }
}
