package com.eastwood.tools.plugins.repo

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class ASMPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
//        def android = project.extensions.getByType(AppExtension)
//        def android = project.extensions.getByType(BaseExtension)
//        if(android!=null){
//            android.registerTransform(new AsmTransform(project))
//        }

        def android = project.getExtensions().findByType(BaseExtension)
        if(android!=null){
            android.registerTransform(new ASMTransform());
        }

    }
}