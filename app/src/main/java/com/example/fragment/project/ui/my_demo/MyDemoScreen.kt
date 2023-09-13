package com.example.fragment.project.ui.my_demo

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fragment.project.R
import com.example.fragment.project.components.ArrowRightItem
import com.example.fragment.project.components.DatePicker
import com.example.fragment.project.components.EllipsisText
import com.example.fragment.project.components.TitleBar
import com.example.miaow.picture.selector.bean.MediaBean
import com.example.miaow.picture.selector.dialog.PictureSelectorCallback
import com.example.miaow.picture.selector.dialog.PictureSelectorDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MyDemoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val fullTextDialog = remember { mutableStateOf(false) }
    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.gray),
                        contentColor = colorResource(R.color.text_666)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "取消", fontSize = 13.sp)
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { scope.launch { sheetState.hide() } },
                    modifier = Modifier
                        .width(50.dp)
                        .height(25.dp),
                    elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.gray)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = colorResource(R.color.theme_orange),
                        contentColor = colorResource(R.color.text_fff)
                    ),
                    contentPadding = PaddingValues(5.dp, 3.dp, 5.dp, 3.dp)
                ) {
                    Text(text = "确定", fontSize = 13.sp)
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            DatePicker(
                onSelectYear = {
                    println("year: $it")
                },
                onSelectMonth = {
                    println("month: $it")
                },
                onSelectDay = {
                    println("day: $it")
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TitleBar(title = "我的Demo") {
                if (context is AppCompatActivity) {
                    context.onBackPressedDispatcher.onBackPressed()
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            ArrowRightItem("日期选择器demo") {
                scope.launch {
                    sheetState.show()
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            ArrowRightItem("图片选择器demo") {
                if (context is AppCompatActivity) {
                    PictureSelectorDialog
                        .newInstance()
                        .setPictureSelectorCallback(object : PictureSelectorCallback {
                            override fun onSelectedData(data: List<MediaBean>) {
                            }
                        })
                        .show(context.supportFragmentManager)
                }
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
            ArrowRightItem("全文demo") {
                fullTextDialog.value = true
            }
            Spacer(
                Modifier
                    .background(colorResource(R.color.line))
                    .fillMaxWidth()
                    .height(1.dp)
            )
        }
        if (fullTextDialog.value) {
            AlertDialog(
                onDismissRequest = { fullTextDialog.value = false },
                title = { Text(text = "全文demo") },
                text = {
                    Column {
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "壬戌之秋1，七月既望2，苏子与客泛舟游于赤壁之下。清风徐来3，水波不兴4。举酒属客5，诵明月之诗6，歌窈窕之章7。少焉8，月出于东山之上，徘徊于斗牛之间9。白露横江10，水光接天。纵一苇之所如，凌万顷之茫然11。浩浩乎如冯虚御风12，而不知其所止；飘飘乎如遗世独立13，羽化而登仙14。\n" +
                                            "于是饮酒乐甚，扣舷而歌之15。歌曰：“桂棹兮兰桨16，击空明兮溯流光17。渺渺兮予怀18，望美人兮天一方19。”客有吹洞箫者，倚歌而和之20。其声呜呜然，如怨如慕21，如泣如诉；余音袅袅22，不绝如缕23。舞幽壑之潜蛟24，泣孤舟之嫠妇25。\n" +
                                            "苏子愀然26，正襟危坐27，而问客曰：“何为其然也28？”\n" +
                                            "客曰：“‘月明星稀，乌鹊南飞。’此非曹孟德之诗乎？西望夏口29，东望武昌30，山川相缪31，郁乎苍苍32，此非孟德之困于周郎者乎33？方其破荆州，下江陵，顺流而东也34，舳舻千里35，旌旗蔽空，酾酒临江36，横槊赋诗37，固一世之雄也，而今安在哉？况吾与子渔樵于江渚之上，侣鱼虾而友麋鹿38，驾一叶之扁舟39，举匏尊（樽）以相属40。寄蜉蝣于天地41，渺沧海之一粟42。哀吾生之须臾43，羡长江之无穷。挟飞仙以遨游，抱明月而长终44。知不可乎骤得45，托遗响于悲风46。” [1]\n" +
                                            "苏子曰：“客亦知夫水与月乎？逝者如斯47，而未尝往也；盈虚者如彼48，而卒莫消长也49。盖将自其变者而观之，则天地曾不能以一瞬50；自其不变者而观之，则物与我皆无尽也，而又何羡乎！且夫天地之间，物各有主，苟非吾之所有，虽一毫而莫取。惟江上之清风，与山间之明月，耳得之而为声，目遇之而成色，取之无禁，用之不竭。是造物者之无尽藏也51，而吾与子之所共适52。”\n" +
                                            "客喜而笑，洗盏更酌。肴核既尽53，杯盘狼籍。相与枕藉乎舟中54，不知东方之既白。"
                                )
                            },
                            fontSize = 14.sp,
                            maxLines = 2,
                            background = colorResource(R.color.white),
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "asdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnmasdfghjklqwertyuiopzxcvbnm"
                                )
                            },
                            fontSize = 14.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
                                )
                            },
                            fontSize = 14.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD我是测试文本123asd,./，。、ASD"
                                )
                            },
                            fontSize = 12.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"
                                )
                            },
                            fontSize = 12.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "asdfgsajkl asgsadhjkl asdfadhjkl asdfgdhjkl asdfgsadhjkl asdfgsadhjkl asdfgsadhjkl asdfgsadhjkl asdfgsadhjkl asdfgsadhjkl asdfgsadhjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl asdfghjkl"
                                )
                            },
                            fontSize = 12.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                        EllipsisText(
                            text = buildAnnotatedString {
                                append(
                                    "你的人生格言是什么？"
                                )
                            },
                            fontSize = 12.sp,
                            background = colorResource(R.color.white),
                            maxLines = 2,
                        )
                        Spacer(
                            Modifier.height(10.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { fullTextDialog.value = false }) { Text("确定") }
                }
            )
        }
    }
}

