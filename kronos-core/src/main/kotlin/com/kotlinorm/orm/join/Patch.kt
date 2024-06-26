/**
* Copyright 2022-2024 kronos-orm
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

package com.kotlinorm.orm.join
 
import com.kotlinorm.beans.dsl.KPojo

inline fun <reified T1 : KPojo,reified T2 : KPojo> T1.join(
    table2: T2,
    selectFrom: SelectFrom2<T1, T2>.(T1, T2) -> Unit
): SelectFrom2<T1, T2> {
    return SelectFrom2(this, table2).apply {
        selectFrom(t1, t2)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    selectFrom: SelectFrom3<T1, T2, T3>.(T1, T2, T3) -> Unit
): SelectFrom3<T1, T2, T3> {
    return SelectFrom3(this, table2, table3).apply {
        selectFrom(t1, t2, t3)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    selectFrom: SelectFrom4<T1, T2, T3, T4>.(T1, T2, T3, T4) -> Unit
): SelectFrom4<T1, T2, T3, T4> {
    return SelectFrom4(this, table2, table3, table4).apply {
        selectFrom(t1, t2, t3, t4)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    selectFrom: SelectFrom5<T1, T2, T3, T4, T5>.(T1, T2, T3, T4, T5) -> Unit
): SelectFrom5<T1, T2, T3, T4, T5> {
    return SelectFrom5(this, table2, table3, table4, table5).apply {
        selectFrom(t1, t2, t3, t4, t5)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    selectFrom: SelectFrom6<T1, T2, T3, T4, T5, T6>.(T1, T2, T3, T4, T5, T6) -> Unit
): SelectFrom6<T1, T2, T3, T4, T5, T6> {
    return SelectFrom6(this, table2, table3, table4, table5, table6).apply {
        selectFrom(t1, t2, t3, t4, t5, t6)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    selectFrom: SelectFrom7<T1, T2, T3, T4, T5, T6, T7>.(T1, T2, T3, T4, T5, T6, T7) -> Unit
): SelectFrom7<T1, T2, T3, T4, T5, T6, T7> {
    return SelectFrom7(this, table2, table3, table4, table5, table6, table7).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    selectFrom: SelectFrom8<T1, T2, T3, T4, T5, T6, T7, T8>.(T1, T2, T3, T4, T5, T6, T7, T8) -> Unit
): SelectFrom8<T1, T2, T3, T4, T5, T6, T7, T8> {
    return SelectFrom8(this, table2, table3, table4, table5, table6, table7, table8).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    selectFrom: SelectFrom9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.(T1, T2, T3, T4, T5, T6, T7, T8, T9) -> Unit
): SelectFrom9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
    return SelectFrom9(this, table2, table3, table4, table5, table6, table7, table8, table9).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    selectFrom: SelectFrom10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10) -> Unit
): SelectFrom10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> {
    return SelectFrom10(this, table2, table3, table4, table5, table6, table7, table8, table9, table10).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    selectFrom: SelectFrom11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) -> Unit
): SelectFrom11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> {
    return SelectFrom11(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo,reified T12 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    table12: T12,
    selectFrom: SelectFrom12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12) -> Unit
): SelectFrom12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> {
    return SelectFrom12(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11, table12).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo,reified T12 : KPojo,reified T13 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    table12: T12,
    table13: T13,
    selectFrom: SelectFrom13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13) -> Unit
): SelectFrom13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> {
    return SelectFrom13(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11, table12, table13).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo,reified T12 : KPojo,reified T13 : KPojo,reified T14 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    table12: T12,
    table13: T13,
    table14: T14,
    selectFrom: SelectFrom14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14) -> Unit
): SelectFrom14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> {
    return SelectFrom14(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11, table12, table13, table14).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo,reified T12 : KPojo,reified T13 : KPojo,reified T14 : KPojo,reified T15 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    table12: T12,
    table13: T13,
    table14: T14,
    table15: T15,
    selectFrom: SelectFrom15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15) -> Unit
): SelectFrom15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> {
    return SelectFrom15(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11, table12, table13, table14, table15).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15)
    }
}
                        


inline fun <reified T1 : KPojo,reified T2 : KPojo,reified T3 : KPojo,reified T4 : KPojo,reified T5 : KPojo,reified T6 : KPojo,reified T7 : KPojo,reified T8 : KPojo,reified T9 : KPojo,reified T10 : KPojo,reified T11 : KPojo,reified T12 : KPojo,reified T13 : KPojo,reified T14 : KPojo,reified T15 : KPojo,reified T16 : KPojo> T1.join(
    table2: T2,
    table3: T3,
    table4: T4,
    table5: T5,
    table6: T6,
    table7: T7,
    table8: T8,
    table9: T9,
    table10: T10,
    table11: T11,
    table12: T12,
    table13: T13,
    table14: T14,
    table15: T15,
    table16: T16,
    selectFrom: SelectFrom16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>.(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16) -> Unit
): SelectFrom16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> {
    return SelectFrom16(this, table2, table3, table4, table5, table6, table7, table8, table9, table10, table11, table12, table13, table14, table15, table16).apply {
        selectFrom(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16)
    }
}
                        