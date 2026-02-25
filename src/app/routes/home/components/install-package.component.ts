import {Component} from "@angular/core";
import {SharedModule} from "../../../shared.module";
import {AvatarModule} from "primeng/avatar";
import {CodemirrorComponent} from "../../../components/codemirror.component";
import {TranslocoPipe} from "@jsverse/transloco";
import {Tab, TabList, Tabs} from "primeng/tabs";

@Component({
    selector: 'install-package',
    imports: [
        SharedModule,
        AvatarModule,
        CodemirrorComponent,
        TranslocoPipe,
        Tabs,
        TabList,
        Tab
    ],
    template: `
        <p-card class="mt-4" [header]="'INSTALL_PACKAGES' | transloco">
            <p-tabs [(value)]="selectedIndex"
                    scrollable selectOnFocus showNavigators>
                <p-tablist>
                    <p-tab [value]="0">
                        <div class="flex items-center gap-2">
                            <p-avatar
                                    image="/assets/icons/gradlekts.svg"
                                    shape="circle"/>
                            <span class="font-bold whitespace-nowrap m-0">
                                Gradle(kts)
                            </span>
                        </div>
                    </p-tab>
                    <p-tab [value]="1">
                        <div class="flex items-center gap-2">
                            <p-avatar
                                    [style.filter]="'invert(1)'"
                                    image="/assets/icons/gradle.svg"
                                    shape="circle"/>
                            <span class="font-bold whitespace-nowrap m-0">
                                Gradle
                            </span>
                        </div>
                    </p-tab>
                    <p-tab [value]="2">
                        <div class="flex items-center gap-2">
                            <p-avatar
                                    image="/assets/icons/maven.svg"
                                    shape="circle"/>
                            <span class="font-bold whitespace-nowrap m-0">
                                Maven
                            </span>
                        </div>
                    </p-tab>
                </p-tablist>
            </p-tabs>
            <codemirror [value]="codes[selectedIndex].code" [language]="codes[selectedIndex].language"/>
        </p-card>
    `,
    styles: `
      :host {
        display: block;
        width: 80%;
        min-width: 400px;
      }

      :host ::ng-deep .p-card-title {
        padding-top: 0.5rem;
      }

      :host ::ng-deep .p-card .p-card-content {
        padding-top: 0 !important;
      }
    `,
    standalone: true
})
export class InstallPackageComponent {
    selectedIndex = 0;
    codes: {
        code: string,
        language: 'kotlin' | 'groovy' | 'xml'
    }[] = [{
        code: `
dependencies {
    implementation("com.kotlinorm.kronos-core:0.0.6")
}

plugins {
    id("com.kotlinorm.kronos-gradle-plugin") version "0.0.6"
}
`.trim(),
        language: 'kotlin'
    }, {
        code: `
dependencies {
    implementation 'com.kotlinorm:kronos-core:0.0.6'
}

plugins {
    id 'com.kotlinorm.kronos-gradle-plugin' version '0.0.6'
}`.trim(),
        language: 'groovy'
    }, {
        code: `
<project>
  <dependencies>
    <dependency>
      <groupId>com.kotlinorm</groupId>
      <artifactId>kronos-core</artifactId>
      <version>0.0.6</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <extensions>true</extensions>
            <configuration>
                <compilerPlugins>
                    <plugin>kronos-maven-plugin</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>com.kotlinorm</groupId>
                    <artifactId>kronos-maven-plugin</artifactId>
                    <version>0.0.6</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
  </build>
</project>
`.trim(),
        language: 'xml'
    }];
}
