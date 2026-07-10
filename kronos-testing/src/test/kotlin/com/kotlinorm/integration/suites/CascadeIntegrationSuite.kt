package com.kotlinorm.integration.suites

import com.kotlinorm.Kronos
import com.kotlinorm.integration.fixtures.CascadeDepartment
import com.kotlinorm.integration.fixtures.CascadeDepartmentRecord
import com.kotlinorm.integration.fixtures.CascadeEmployee
import com.kotlinorm.integration.fixtures.CascadeEmployeeRecord
import com.kotlinorm.integration.fixtures.CascadeInsertDepartment
import com.kotlinorm.integration.fixtures.CascadeInsertEmployee
import com.kotlinorm.integration.fixtures.CascadeProject
import com.kotlinorm.integration.fixtures.CascadeProjectRecord
import com.kotlinorm.integration.fixtures.CascadeTask
import com.kotlinorm.integration.fixtures.CascadeTaskRecord
import com.kotlinorm.integration.profiles.IntegrationScenarioProfile
import com.kotlinorm.integration.support.IntegrationDatabaseEnvironment
import com.kotlinorm.integration.support.IntegrationSuiteSupport
import com.kotlinorm.orm.ddl.table
import com.kotlinorm.orm.delete.delete
import com.kotlinorm.orm.insert.insert
import com.kotlinorm.orm.select.select
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CascadeIntegrationSuite(
    environment: IntegrationDatabaseEnvironment,
    profile: IntegrationScenarioProfile,
) : IntegrationSuiteSupport(environment, profile) {
    @Test
    fun cascadeInsertAndSelectCollectionExecuteAgainstRealDatabase() {
        recreateCascadeTables()

        CascadeInsertDepartment(
            name = "Engineering",
            employees = listOf(
                CascadeInsertEmployee(name = "Ada"),
                CascadeInsertEmployee(name = "Grace"),
            ),
        ).insert()
            .cascade { [CascadeInsertDepartment::employees] }
            .execute()

        assertEquals(
            listOf(
                CascadeEmployeeRecord(id = 1, departmentId = 1, name = "Ada"),
                CascadeEmployeeRecord(id = 2, departmentId = 1, name = "Grace"),
            ),
            selectInsertEmployeeRecords(),
        )
        assertEquals(
            listOf(
                CascadeDepartmentRecord(
                    id = 1,
                    name = "Engineering",
                    employees = listOf(
                        CascadeEmployeeRecord(id = 1, departmentId = 1, name = "Ada"),
                        CascadeEmployeeRecord(id = 2, departmentId = 1, name = "Grace"),
                    ),
                )
            ),
            selectInsertDepartmentRecords(),
        )
    }

    @Test
    fun cascadeSelectOnlyRunsWhenRequestedAgainstRealDatabase() {
        recreateCascadeTables()
        seedTwoCascadeInsertDepartments()

        assertEquals(
            listOf(
                CascadeDepartmentRecord(id = 1, name = "Engineering", employees = emptyList()),
                CascadeDepartmentRecord(id = 2, name = "Research", employees = emptyList()),
            ),
            selectInsertDepartmentRecordsWithoutCascade(),
        )
        assertEquals(
            listOf(
                CascadeDepartmentRecord(
                    id = 1,
                    name = "Engineering",
                    employees = listOf(
                        CascadeEmployeeRecord(id = 1, departmentId = 1, name = "Ada"),
                        CascadeEmployeeRecord(id = 2, departmentId = 1, name = "Grace"),
                    ),
                ),
                CascadeDepartmentRecord(
                    id = 2,
                    name = "Research",
                    employees = listOf(
                        CascadeEmployeeRecord(id = 3, departmentId = 2, name = "Barbara"),
                    ),
                ),
            ),
            selectInsertDepartmentRecords(),
        )
    }

    @Test
    fun cascadeSelectMultipleParentsKeepsChildGroupsAgainstRealDatabase() {
        recreateCascadeTables()
        seedTwoCascadeInsertDepartments()

        assertEquals(
            listOf(
                CascadeEmployeeRecord(id = 1, departmentId = 1, name = "Ada"),
                CascadeEmployeeRecord(id = 2, departmentId = 1, name = "Grace"),
                CascadeEmployeeRecord(id = 3, departmentId = 2, name = "Barbara"),
            ),
            selectInsertEmployeeRecords(),
        )
        assertEquals(
            listOf(
                CascadeDepartmentRecord(
                    id = 1,
                    name = "Engineering",
                    employees = listOf(
                        CascadeEmployeeRecord(id = 1, departmentId = 1, name = "Ada"),
                        CascadeEmployeeRecord(id = 2, departmentId = 1, name = "Grace"),
                    ),
                ),
                CascadeDepartmentRecord(
                    id = 2,
                    name = "Research",
                    employees = listOf(
                        CascadeEmployeeRecord(id = 3, departmentId = 2, name = "Barbara"),
                    ),
                ),
            ),
            selectInsertDepartmentRecords(),
        )
    }

    @Test
    fun cascadeDeleteSetNullKeepsChildRowsAgainstRealDatabase() {
        recreateCascadeTables()
        CascadeDepartment(id = 20, name = "Support").insert().execute()
        CascadeEmployee(id = 201, departmentId = 20, name = "Linus").insert().execute()
        CascadeEmployee(id = 202, departmentId = 20, name = "Ken").insert().execute()

        assertEquals(3, CascadeDepartment(id = 20).delete().logic(false).by { it.id }.execute().affectedRows)

        assertEquals(emptyList(), selectDepartmentRecords())
        assertEquals(
            listOf(
                CascadeEmployeeRecord(id = 201, departmentId = null, name = "Linus"),
                CascadeEmployeeRecord(id = 202, departmentId = null, name = "Ken"),
            ),
            selectEmployeeRecords(),
        )
    }

    @Test
    fun cascadeDeleteCascadeRemovesChildRowsAgainstRealDatabase() {
        recreateCascadeTables()
        CascadeProject(id = 30, name = "Compiler").insert().execute()
        CascadeTask(id = 301, projectId = 30, name = "Parser").insert().execute()
        CascadeTask(id = 302, projectId = 30, name = "Planner").insert().execute()

        assertEquals(3, CascadeProject(id = 30).delete().logic(false).by { it.id }.execute().affectedRows)

        assertEquals(emptyList(), selectProjectRecords())
        assertEquals(emptyList(), selectTaskRecords())
    }

    private fun seedTwoCascadeInsertDepartments() {
        CascadeInsertDepartment(
            name = "Engineering",
            employees = listOf(
                CascadeInsertEmployee(name = "Ada"),
                CascadeInsertEmployee(name = "Grace"),
            ),
        ).insert()
            .cascade { [CascadeInsertDepartment::employees] }
            .execute()
        CascadeInsertDepartment(
            name = "Research",
            employees = listOf(
                CascadeInsertEmployee(name = "Barbara"),
            ),
        ).insert()
            .cascade { [CascadeInsertDepartment::employees] }
            .execute()
    }

    private fun recreateCascadeTables() {
        assumeDatabaseAvailable()
        configureKronos()
        with(Kronos.dataSource().table) {
            dropTable(CascadeTask())
            dropTable(CascadeProject())
            dropTable(CascadeEmployee())
            dropTable(CascadeDepartment())
            dropTable(CascadeInsertEmployee())
            dropTable(CascadeInsertDepartment())
            syncTable(CascadeInsertDepartment())
            syncTable(CascadeInsertEmployee())
            syncTable(CascadeDepartment())
            syncTable(CascadeEmployee())
            syncTable(CascadeProject())
            syncTable(CascadeTask())
            truncateTable(CascadeTask(), restartIdentity = restartIdentity)
            truncateTable(CascadeProject(), restartIdentity = restartIdentity)
            truncateTable(CascadeEmployee(), restartIdentity = restartIdentity)
            truncateTable(CascadeDepartment(), restartIdentity = restartIdentity)
            truncateTable(CascadeInsertEmployee(), restartIdentity = restartIdentity)
            truncateTable(CascadeInsertDepartment(), restartIdentity = restartIdentity)
        }
        assertEquals(emptyList(), selectInsertDepartmentRecords())
        assertEquals(emptyList(), selectInsertEmployeeRecords())
        assertEquals(emptyList(), selectDepartmentRecords())
        assertEquals(emptyList(), selectEmployeeRecords())
        assertEquals(emptyList(), selectProjectRecords())
        assertEquals(emptyList(), selectTaskRecords())
    }

    private fun selectInsertDepartmentRecords(): List<CascadeDepartmentRecord> =
        CascadeInsertDepartment()
            .select()
            .cascade { [CascadeInsertDepartment::employees] }
            .orderBy { it.id.asc() }
            .toList<CascadeInsertDepartment>()
            .map { department ->
                CascadeDepartmentRecord(
                    id = department.id,
                    name = department.name,
                    employees = department.employees
                        .map { CascadeEmployeeRecord(id = it.id, departmentId = it.departmentId, name = it.name) }
                        .sortedBy { it.id },
                )
            }

    private fun selectInsertDepartmentRecordsWithoutCascade(): List<CascadeDepartmentRecord> =
        CascadeInsertDepartment()
            .select()
            .cascade(false)
            .orderBy { it.id.asc() }
            .toList<CascadeInsertDepartment>()
            .map { department ->
                CascadeDepartmentRecord(
                    id = department.id,
                    name = department.name,
                    employees = department.employees
                        .map { CascadeEmployeeRecord(id = it.id, departmentId = it.departmentId, name = it.name) }
                        .sortedBy { it.id },
                )
            }

    private fun selectInsertEmployeeRecords(): List<CascadeEmployeeRecord> =
        CascadeInsertEmployee()
            .select { [it.id, it.departmentId, it.name] }
            .orderBy { it.id.asc() }
            .toList<CascadeInsertEmployee>()
            .map { CascadeEmployeeRecord(id = it.id, departmentId = it.departmentId, name = it.name) }

    private fun selectDepartmentRecords(): List<CascadeDepartmentRecord> =
        CascadeDepartment()
            .select()
            .cascade { [CascadeDepartment::employees] }
            .orderBy { it.id.asc() }
            .toList<CascadeDepartment>()
            .map { department ->
                CascadeDepartmentRecord(
                    id = department.id,
                    name = department.name,
                    employees = department.employees
                        .map { CascadeEmployeeRecord(id = it.id, departmentId = it.departmentId, name = it.name) }
                        .sortedBy { it.id },
                )
            }

    private fun selectEmployeeRecords(): List<CascadeEmployeeRecord> =
        CascadeEmployee()
            .select { [it.id, it.departmentId, it.name] }
            .orderBy { it.id.asc() }
            .toList<CascadeEmployee>()
            .map { CascadeEmployeeRecord(id = it.id, departmentId = it.departmentId, name = it.name) }

    private fun selectProjectRecords(): List<CascadeProjectRecord> =
        CascadeProject()
            .select()
            .cascade { [CascadeProject::tasks] }
            .orderBy { it.id.asc() }
            .toList<CascadeProject>()
            .map { project ->
                CascadeProjectRecord(
                    id = project.id,
                    name = project.name,
                    tasks = project.tasks
                        .map { CascadeTaskRecord(id = it.id, projectId = it.projectId, name = it.name) }
                        .sortedBy { it.id },
                )
            }

    private fun selectTaskRecords(): List<CascadeTaskRecord> =
        CascadeTask()
            .select { [it.id, it.projectId, it.name] }
            .orderBy { it.id.asc() }
            .toList<CascadeTask>()
            .map { CascadeTaskRecord(id = it.id, projectId = it.projectId, name = it.name) }
}
