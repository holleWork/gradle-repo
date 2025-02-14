package com.eastwood.tools.plugins.repo


import com.android.build.gradle.BaseExtension

import org.gradle.api.Plugin
import org.gradle.api.Project


class ASMPlugin2 implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def android = project.getExtensions().findByType(BaseExtension)
        if(android!=null){
            android.registerTransform(new ASMTransform2());
        }


    }
}

