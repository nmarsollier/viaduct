package viaduct;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BuildLogicUtilsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getExtensions().create("buildLogicUtils", BuildLogicUtilsExtension.class);
    }
}
