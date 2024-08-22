package com.example.miaow.base.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.miaow.base.provider.BaseContentProvider
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.RandomAccessFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.Locale

object FileUtil {

    fun isSDCardAlive(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun delete(file: File?) {
        file?.let {
            if (it.isDirectory) {
                it.listFiles()?.let { list ->
                    for (i in list) {
                        delete(i)
                    }
                }
            } else {
                it.delete()
            }
        }
    }

    fun getSize(file: File): Long {
        var size: Long = 0
        try {
            file.listFiles()?.apply {
                for (f in this) {
                    size = if (f.isDirectory) {
                        size + getSize(f)
                    } else {
                        size + f.length()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return size
    }

    fun formatSize(size: Double): String {
        val kiloByte = size / 1000
        if (kiloByte < 1) {
            return "0KB"
        }
        val megaByte = kiloByte / 1000
        if (megaByte < 1) {
            val result = BigDecimal(kiloByte.toString())
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "KB"
        }
        val gigaByte = megaByte / 1000
        if (gigaByte < 1) {
            val result = BigDecimal(megaByte.toString())
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "MB"
        }
        val teraBytes = gigaByte / 1000
        if (teraBytes < 1) {
            val result = BigDecimal(gigaByte.toString())
            return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "GB"
        }
        val result = BigDecimal(teraBytes)
        return result.setScale(2, RoundingMode.HALF_UP).toPlainString() + "TB"
    }

    private const val BINARY_SEPARATOR = " "

    //字符串转换为二进制字符串
    fun strToBinary(str: String): String {
        val sb = StringBuilder()
        val bytes = str.toByteArray()
        for (aByte in bytes) {
            sb.append(Integer.toBinaryString(aByte.toInt())).append(BINARY_SEPARATOR)
        }
        return sb.toString()
    }

    //二进制字符串转换为普通字符串
    fun binaryToStr(binaryStr: String): String {
        val sb = StringBuilder()
        val binArrays = binaryStr.split(BINARY_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (binStr in binArrays) {
            val c = binaryToChar(binStr)
            sb.append(c)
        }
        return sb.toString()
    }

    //二进制字符转换为int数组
    private fun binaryToIntArray(binaryStr: String): IntArray {
        val temp = binaryStr.toCharArray()
        val result = IntArray(temp.size)
        for (i in temp.indices) {
            result[i] = temp[i].code - 48
        }
        return result
    }

    // 将二进制转换成字符
    private fun binaryToChar(binaryStr: String): Char {
        val temp = binaryToIntArray(binaryStr)
        var sum = 0
        for (i in temp.indices) {
            sum += temp[temp.size - 1 - i] shl i
        }
        return sum.toChar()
    }

    /**
     * 写入脏数据
     */
    fun writeDirtyRead(destFile: File) {
        Thread {
            while (true) {
                if (getAvailableStorage() > 1000 * 1000)
                    writeToFile(
                        "第八十一回 急兄仇张飞遇害 雪弟恨先主兴兵" +
                                " \n" +
                                "却说先主欲起兵东征，赵云谏曰：“国贼乃曹操，非孙权也。今曹丕篡汉，神人共怒。陛下可早图关中，屯兵渭河上流，以讨凶逆，则关东义士，必裹粮策马以迎王师；若舍魏以伐吴，兵势一交，岂能骤解。愿陛下察之。”先主曰：“孙权害了朕弟；又兼傅士仁、糜芳、潘璋、马忠皆有切齿之仇：啖其肉而灭其族，方雪朕恨！卿何阻耶？”云曰：“汉贼之仇，公也；兄弟之仇，私也。愿以天下为重。”先主答曰：“朕不为弟报仇，虽有万里江山，何足为贵？”遂不听赵云之谏，下令起兵伐吴；且发使往五溪，借番兵五万，共相策应；一面差使往阆中，迁张飞为车骑将军，领司隶校尉，封西乡侯，兼阆中牧。使命赍诏而去。\n" +
                                " \n" +
                                "却说张飞在阆中，闻知关公被东吴所害，旦夕号泣，血湿衣襟。诸将以酒解劝，酒醉，怒气愈加。帐上帐下，但有犯者即鞭挞之；多有鞭死者。每日望南切齿睁目怒恨，放声痛哭不已。忽报使至，慌忙接入，开读诏旨。飞受爵望北拜毕，设酒款待来使。飞曰：“吾兄被害，仇深似海；庙堂之臣，何不早奏兴兵？”使者曰：“多有劝先灭魏而后伐吴者。”飞怒曰：“是何言也！昔我三人桃园结义，誓同生死；今不幸二兄半途而逝，吾安得独享富贵耶！吾当面见天子，愿为前部先锋，挂孝伐吴，生擒逆贼，祭告二兄，以践前盟！”言讫，就同使命望成都而来。\n" +
                                " \n" +
                                "却说先主每日自下教场操演军马，克日兴师，御驾亲征。于是公卿都至丞相府中见孔明，曰：“今天子初临大位，亲统军伍，非所以重社稷也。丞相秉钧衡之职，何不规谏？”孔明曰：“吾苦谏数次，只是不听。今日公等随我入教场谏去。”当下孔明引百官来奏先主曰：“陛下初登宝位，若欲北讨汉贼，以伸大义于天下，方可亲统六师；若只欲伐吴，命一上将统军伐之可也，何必亲劳圣驾？”先主见孔明苦谏，心中稍回。忽报张飞到来，先主急召入。飞至演武厅拜伏于地，抱先主足而哭。先主亦哭。飞曰：“陛下今日为君，早忘了桃园之誓！二兄之仇，如何不报？”先主曰：“多官谏阻，未敢轻举。”飞曰：“他人岂知昔日之盟？若陛下不去，臣舍此躯与二兄报仇！若不能报时，臣宁死不见陛下也！”先主曰：“朕与卿同往：卿提本部兵自阆州而出，朕统精兵会于江州，共伐东吴，以雪此恨！”飞临行，先主嘱曰：“朕素知卿酒后暴怒，鞭挞健儿，而复令在左右：此取祸之道也。今后务宜宽容，不可如前。”飞拜辞而去。\n" +
                                " \n" +
                                "次日，先主整兵要行。学士秦宓奏曰：“陛下舍万乘之躯，而徇小义，古人所不取也。愿陛下思之。”先主曰：“云长与朕，犹一体也。大义尚在，岂可忘耶？”宓伏地不起曰：“陛下不从臣言，诚恐有失。”先主大怒曰：“朕欲兴兵，尔何出此不利之言！”叱武士推出斩之，宓面不改色，回顾先主而笑曰：“臣死无恨，但可惜新创之业，又将颠覆耳！”众官皆为秦宓告免。先主曰：“暂且囚下，待朕报仇回时发落。”孔明闻知，即上表救秦宓。其略曰：\n" +
                                " \n" +
                                "臣亮等窃以吴贼逞奸诡之计，致荆州有覆亡之祸；陨将星于斗牛，折天柱于楚地：此情哀痛，诚不可忘。但念迁汉鼎者，罪由曹操；移刘祚者，过非孙权。窃谓魏贼若除，则吴自宾服。愿陛下纳秦宓金石之言，以养士卒之力，别作良图，则社稷幸甚！天下幸甚！\n" +
                                " \n" +
                                "先主看毕，掷表于地曰：“朕意已决，无得再谏！”遂命丞相诸葛亮保太子守两川；骠骑将军马超并弟马岱，助镇北将军魏延守汉中，以当魏兵；虎威将军赵云为后应，兼督粮草；黄权、程畿为参谋；马良、陈震掌理文书；黄忠为前部先锋；冯习、张南为副将；傅彤、张翼为中军护尉；赵融、廖淳为合后。川将数百员，并五溪番将等，共兵七十五万，择定章武元年七月丙寅日出师。\n" +
                                " \n" +
                                "却说张飞回到阆中，下令军中；限三日内制办白旗白甲，三军挂孝伐吴。次日，帐下两员末将范疆、张达，入帐告曰：“白旗白甲，一时无措，须宽限方可。飞大怒曰：“吾急欲报仇，恨不明日便到逆贼之境，汝安敢违我将令！”叱武士缚于树上，各鞭背五十。鞭毕，以手指之曰：“来日俱要完备！若违了限，即杀汝二人示众！”打得二人满口出血。回到营中商议，范疆曰：“今日受了刑责，着我等如何办得？其人性暴如火，倘来日不完，你我皆被杀矣！”张达曰：“比如他杀我，不如我杀他。”疆曰：“怎奈不得近前。”达曰：“我两个若不当死，则他醉于床上；若是当死，则他不醉。”二人商议停当。\n" +
                                " \n" +
                                "却说张飞在帐中，神思昏乱，动止恍惚，乃问部将曰：“吾今心惊肉颠，坐卧不安，此何意也？”部将答曰：“此是君侯思念关公，以致如此。”飞令人将酒来，与部将同饮，不觉大醉，卧于帐中。范、张二贼，探知消息，初更时分，各藏短刀，密入帐中，诈言欲禀机密重事，直至床前。原来张飞每睡不合眼；当夜寝于帐中，二贼见他须竖目张，本不敢动手。因闻鼻息如雷，方敢近前，以短刀刺入飞腹。飞大叫一声而亡。时年五十五岁。后人有诗叹曰：\n" +
                                " \n" +
                                "安喜曾闻鞭督邮，黄巾扫尽佐炎刘。虎牢关上声先震，长坂桥边水逆流。\n" +
                                " \n" +
                                "义释严颜安蜀境，智欺张郃定中州。伐吴未克身先死，秋草长遗阆地愁。\n" +
                                " \n" +
                                "却说二贼当夜割了张飞首级，便引数十人连夜投东吴去了。次日，军中闻知，起兵追之不及。时有张飞部将吴班，向自荆州来见先主，先主用为牙门将，使佐张飞守阆中。当下吴班先发表章，奏知天子；然后令长子张苞具棺椁盛贮，令弟张绍守阆中，苞自来报先主。时先主已择期出师。大小官僚，皆随孔明送十里方回。孔明回至成都，怏怏不乐，顾谓众官曰：“法孝直若在，必能制主上东行也。”\n" +
                                " \n" +
                                "却说先主是夜心惊肉颤，寝卧不安。出帐仰观天文，见西北一星，其大如斗，忽然坠地。先主大疑，连夜令人求问孔明。孔明回奏曰：“合损一上将。三日之内，必有惊报。”先主因此按兵不动。忽侍臣奏曰：“阆中张车骑部将吴班，差人赍表至。”先主顿足曰：“噫！三弟休矣！”及至览表，果报张飞凶信。先主放声大哭，昏绝于地。众官救醒。\n" +
                                " \n" +
                                "次日，人报一队军马骤风而至。先主出营观之。良久，见一员小将，白袍银铠，滚鞍下马，伏地而哭，乃张苞也。苞曰：“范疆、张达杀了臣父，将首级投吴去了！”先主哀痛至甚，饮食不进。群臣苦谏曰：“陛下方欲为二弟报仇，何可先自摧残龙体？”先主方才进膳，遂谓张苞曰：“卿与吴班，敢引本部军作先锋，为卿父报仇否？”苞曰：“为国为父，万死不辞！”先主正欲遣苞起兵，又报一彪军风拥而至。先主令侍臣探之。\n" +
                                " \n" +
                                "须臾，侍臣引一小将军，白袍银铠，入营伏地而哭。先主视之，乃关兴也。先主见了关兴，想起关公，又放声大哭。众官苦劝。先主曰：“朕想布衣时，与关、张结义，誓同生死；今朕为天子，正欲与两弟同享富贵，不幸俱死于非命！见此二侄，能不断肠！”言讫又哭。众官曰：“二小将军且退。容圣上将息龙体。”侍臣奏曰：“陛下年过六旬，不宜过于哀痛。”先主曰：“二弟俱亡，朕安忍独生！”言讫，以头顿地而哭。\n" +
                                " \n" +
                                "多官商议曰：“今天子如此烦恼，将何解劝？”马良曰：“主上亲统大兵伐吴，终日号泣，于军不利。”陈震曰：“吾闻成都青城山之西，有一隐者，姓李，名意。世人传说此老已三百余岁，能知人之生死吉凶，乃当世之神仙也。何不奏知天子，召此老来，问他吉凶，胜如吾等之言。”遂入奏先主。先主从之，即遣陈震赍诏，往青城山宣召。震星夜到了青城，令乡人引入出谷深处，遥望仙庄，清云隐隐，瑞气非凡。忽见一小童来迎曰：“来者莫非陈孝起乎？”震大惊曰：“仙童如何知我姓字！”童子曰：“吾师昨者有言：今日必有皇帝诏命至；使者必是陈孝起。”震曰：“真神仙也！人言信不诬矣！”遂与小童同入仙庄，拜见李意，宣天子诏命。李意推老不行。震曰：“天子急欲见仙翁一面，幸勿吝鹤驾。”再三敦请，李意方行。\n" +
                                " \n" +
                                "即至御营，入见先主。先主见李意鹤发童颜，碧眼方瞳，灼灼有光，身如古柏之状，知是异人，优礼相待。李意曰：“老夫乃荒山村叟，无学无识。辱陛下宣召，不知有何见谕？”先主曰：“朕与关、张二弟生死之交，三十余年矣。今二弟被害，亲统大军报仇，未知休咎如何。久闻仙翁通晓玄机，望乞赐教。”李意曰：“此乃天数，非老夫所知也。”先主再三求问，意乃索纸笔画兵马器械四十余张，画毕便一一扯碎。又画一大人仰卧于地上，傍边一人掘土埋之，上写一大“白”字，遂稽首而去。先主不悦，谓群臣曰：“此狂叟也！不足为信。”即以火焚之，便催军前进。\n" +
                                " \n" +
                                "张苞入奏曰：“吴班军马已至。小臣乞为先锋。”先主壮其志，即取先锋印赐张苞。苞方欲挂印，又一少年将奋然出曰：“留下印与我！”视之，乃关兴也。苞曰：“我已奉诏矣。”兴曰：“汝有何能，敢当此任？”苞曰：“我自幼习学武艺，箭无虚发。”先主曰：“朕正要观贤侄武艺，以定优劣。”苞令军士于百步之外，立一面旗，旗上画一红心。苞拈弓取箭，连射三箭，皆中红心。众皆称善。关兴挽弓在手曰：“射中红心何足为奇？”正言间，忽值头上一行雁过。兴指曰：“吾射这飞雁第三只。”一箭射去，那只雁应弦而落。文武官僚，齐声喝采。苞大怒，飞身上马，手挺父所使丈八点钢矛，大叫曰：“你敢与我比试武艺否？”兴亦上马，绰家传大砍刀纵马而出曰：“偏你能使矛！吾岂不能使刀！”\n" +
                                " \n" +
                                "二将方欲交锋，先主喝曰：“二子休得无礼！”兴、苞二人慌忙下马，各弃兵器，拜伏请罪。先主曰：“朕自涿郡与卿等之父结异姓之交，亲如骨肉；今汝二人亦是昆仲之分，正当同心协力，共报父仇；奈何自相争竞，失其大义！父丧未远而犹如此，况日后乎？”二人再拜伏罪。先主问曰：“卿二人谁年长？”苞曰：“臣长关兴一岁。”先主即命兴拜苞为兄。二人就帐前折箭为誓，永相救护。先主下诏使吴班为先锋，令张苞、关兴护驾。水陆并进，船骑双行，浩浩荡荡，杀奔吴国来。\n" +
                                " \n" +
                                "却说范疆、张达将张飞首级，投献吴侯，细告前事。孙权听罢，收了二人，乃谓百官曰：“今刘玄德即了帝位，统精兵七十余万，御驾亲征，其势甚大，如之奈何？”百官尽皆失色，面面相觑。诸葛瑾出曰：“某食君侯之禄久矣，无可报效，愿舍残生，去见蜀主，以利害说之，使两国相和，共讨曹丕之罪。”权大喜，即遣诸葛瑾为使，来说先主罢兵。正是：\n" +
                                " \n" +
                                "两国相争通使命，一言解难赖行人。" +
                                " \n" +
                                "第一百零四回 陨大星汉丞相归天 见木像魏都督丧" +
                                " \n" +
                                "却说姜维见魏延踏灭了灯，心中忿怒，拔剑欲杀之。孔明止之曰：“此吾命当绝，非文长之过也。”维乃收剑。孔明吐血数口，卧倒床上，谓魏延曰：“此是司马懿料吾有病，故令人来探视虚实。汝可急出迎敌。”魏延领命，出帐上马，引兵杀出寨来。夏侯霸见了魏延，慌忙引军退走。延追赶二十余里方回。孔明令魏延自回本寨把守。\n" +
                                " \n" +
                                "姜维入帐，直至孔明榻前问安。孔明曰：“吾本欲竭忠尽力，恢复中原，重兴汉室；奈天意如此，吾旦夕将死。吾平生所学，已著书二十四篇，计十万四千一百一十二字，内有八务、七戒、六恐、五惧之法。吾遍观诸将，无人可授，独汝可传我书。切勿轻忽！”维哭拜而受。孔明又曰：“吾有‘连弩’之法，不曾用得。其法矢长八寸，一弩可发十矢，皆画成图本。汝可依法造用。”维亦拜受。孔明又曰：“蜀中诸道，皆不必多忧；惟阴平之地，切须仔细。此地虽险峻，久必有失。”又唤马岱入帐，附耳低言，授以密计；嘱曰：“我死之后，汝可依计行之。”\n" +
                                " \n" +
                                "岱领计而出。少顷，杨仪入。孔明唤至榻前，授与一锦囊，密嘱曰：“我死，魏延必反；待其反时，汝与临阵，方开此囊。那时自有斩魏延之人也。”孔明一一调度已毕，便昏然而倒，至晚方苏，便连夜表奏后主。后主闻奏大惊，急命尚书李福，星夜至军中问安，兼询后事。李福领命，趱程赴五丈原，入见孔明，传后主之命，问安毕。孔明流涕曰：“吾不幸中道丧亡，虚废国家大事，得罪于天下。我死后，公等宜竭忠辅主。国家旧制，不可改易；吾所用之人，亦不可轻废。吾兵法皆授与姜维，他自能继吾之志，为国家出力。吾命已在旦夕，当即有遗表上奏天子也。”李福领了言语，匆匆辞去。\n" +
                                " \n" +
                                "孔明强支病体，令左右扶上小车，出寨遍观各营；自觉秋风吹面，彻骨生寒，乃长叹曰：“再不能临阵讨贼矣！悠悠苍天，曷此其极！”叹息良久。回到帐中，病转沉重，乃唤杨仪分付曰：“王平、廖化、张嶷、张翼、吴懿等，皆忠义之士，久经战阵，多负勤劳，堪可委用。我死之后，凡事俱依旧法而行。缓缓退兵，不可急骤。汝深通谋略，不必多嘱。姜伯约智勇足备，可以断后。”杨仪泣拜受命。孔明令取文房四宝，于卧榻上手书遗表，以达后主。表略曰：\n" +
                                " \n" +
                                "伏闻生死有常，难逃定数；死之将至，愿尽愚忠：臣亮赋性愚拙，遭时艰难，分符拥节，专掌钧衡，兴师北伐，未获成功；何期病入膏肓，命垂旦夕，不及终事陛下，饮恨无穷！伏愿陛下：清心寡欲，约己爱民；达孝道于先皇，布仁恩于宇下；提拔幽隐，以进贤良；屏斥奸邪，以厚风俗。\n" +
                                " \n" +
                                "臣家成都有桑八百株，薄田十五顷，子弟衣食，自有余饶。至于臣在外任，别无调度，随身衣食，悉仰于官，不别治生，以长尺寸。臣死之日，不使内有余帛，外有赢财，以负陛下也。\n" +
                                " \n" +
                                "孔明写毕，又嘱杨仪曰：“吾死之后，不可发丧。可作一大龛，将吾尸坐于龛中；以米七粒，放吾口内；脚下用明灯一盏；军中安静如常，切勿举哀：则将星不坠。吾阴魂更自起镇之。司马懿见将星不坠，必然惊疑。吾军可令后寨先行，然后一营一营缓缓而退。若司马懿来追，汝可布成阵势，回旗返鼓。等他来到，却将我先时所雕木像，安于车上，推出军前，令大小将士，分列左右。懿见之必惊走矣。”杨仪一一领诺。\n" +
                                " \n" +
                                "是夜，孔明令人扶出，仰观北斗，遥指一星曰：“此吾之将星也。”众视之，见其色昏暗，摇摇欲坠。孔明以剑指之，口中念咒。咒毕急回帐时，不省人事。众将正慌乱间，忽尚书李福又至；见孔明昏绝，口不能言，乃大哭曰：“我误国家之大事也！”须臾，孔明复醒，开目遍视，见李福立于榻前。孔明曰：“吾已知公复来之意。福谢曰：“福奉天子命，问丞相百年后，谁可任大事者。适因匆遽，失于谘请，故复来耳。”孔明曰：“吾死之后，可任大事者：蒋公琰其宜也。”福曰：“公琰之后，谁可继之？”孔明曰：“费文伟可继之。”福又问：“文伟之后，谁当继者？”孔明不答。众将近前视之，已薨矣。时建兴十二年秋八月二十三日也，寿五十四岁。后杜工部有诗叹曰：\n" +
                                " \n" +
                                "长星昨夜坠前营，讣报先生此日倾。虎帐不闻施号令，麟台惟显著勋名。\n" +
                                " \n" +
                                "空余门下三千客，辜负胸中十万兵。好看绿阴清昼里，于今无复雅歌声！\n" +
                                " \n" +
                                "白乐天亦有诗曰：\n" +
                                " \n" +
                                "先生晦迹卧山林，三顾那逢圣主寻。鱼到南阳方得水，龙飞天汉便为霖。\n" +
                                " \n" +
                                "托孤既尽殷勤礼，报国还倾忠义心。前后出师遗表在，令人一览泪沾襟。\n" +
                                " \n" +
                                "初，蜀长水校尉廖立，自谓才名宜为孔明之副，尝以职位闲散，怏怏不平，怨谤无已。于是孔明废之为庶人，徒之汶山。及闻孔明亡，乃垂泣曰：“吾终为左衽矣！”李严闻之，亦大哭病死，盖严尝望孔明复收己，得自补前过；度孔明死后，人不能用之故也。后元微之有赞孔明诗曰：\n" +
                                " \n" +
                                "拨乱扶危主，殷勤受托孤。英才过管乐，妙策胜孙吴。\n" +
                                " \n" +
                                "凛凛《出师表》，堂堂八阵图。如公全盛德，应叹古今无！\n" +
                                " \n" +
                                "是夜，天愁地惨，月色无光，孔明奄然归天。姜维、杨仪遵孔明遗命，不敢举哀，依法成殓，安置龛中，令心腹将卒三百人守护；随传密令，使魏延断后，各处营寨一一退去。\n" +
                                " \n" +
                                "却说司马懿夜观天文，见一大星，赤色，光芒有角，自东北方流于西南方，坠于蜀营内，三投再起，隐隐有声。懿惊喜曰：“孔明死矣！”即传令起大兵追之。方出寨门，忽又疑虑曰：“孔明善会六丁六甲之法，今见我久不出战，故以此术诈死，诱我出耳。今若追之，必中其计。”遂复勒马回寨不出，只令夏侯霸暗引数十骑，往五丈原山僻哨探消息。\n" +
                                " \n" +
                                "却说魏延在本寨中，夜作一梦，梦见头上忽生二角，醒来甚是疑异。次日，行军司马赵直至，延请入问曰：“久知足下深明《易》理，吾夜梦头生二角，不知主何吉凶？烦足下为我决之。”赵直想了半晌，答曰：“此大吉之兆：麒麟头上有角，苍龙头上有角，乃变化飞腾之象也。”延大喜曰：“如应公言，当有重谢！”直辞去，行不数里，正遇尚书费祎。祎问何来。直曰：“适至魏文长营中，文长梦头生角，令我决其吉凶。此本非吉兆，但恐直言见怪，因以麒麟苍龙解之。”祎曰：“足下何以知非吉兆？”直曰：“角之字形，乃刀下用也。今头上用刀，其凶甚矣！”祎曰：“君且勿泄漏。”直别去。费祎至魏延寨中，屏退左右，告曰：“昨夜三更，丞相已辞世矣。临终再三嘱付，令将军断后以当司马懿，缓缓而退，不可发丧。今兵符在此，便可起兵。”延曰：“何人代理丞相之大事？”祎曰：“丞相一应大事，尽托与杨仪；用兵密法，皆授与姜伯约。此兵符乃杨仪之令也。”延曰：“丞相虽亡，吾今现在。杨仪不过一长史，安能当此大任？他只宜扶柩入川安葬。我自率大兵攻司马懿，务要成功。岂可因丞相一人而废国家大事耶？”祎曰：“丞相遗令，教且暂退，不可有违。”延怒曰：“丞相当时若依我计，取长安久矣！吾今官任前将军、征西大将军、南郑侯，安肯与长史断后！“祎曰：“将军之言虽是，然不可轻动，令敌人耻笑。待吾往见杨仪，以利害说之，令彼将兵权让与将军，何如？”延依其言。\n" +
                                " \n" +
                                "祎辞延出营，急到大寨见杨仪，具述魏延之语。仪曰：“丞相临终，曾密嘱我曰：魏延必有异志。今我以兵符往，实欲探其心耳。今果应丞相之言。吾自令伯约断后可也。”于是杨仪领兵扶柩先行，令姜维断后；依孔明遗令，徐徐而退。魏延在寨中，不见费祎来回覆，心中疑惑，乃令马岱引十数骑往探消息。回报曰：“后军乃姜维总督，前军大半退入谷中去了。”延大怒曰：“竖儒安敢欺我！我必杀之！”因顾谓岱曰：“公肯相助否？”岱曰：“某亦素恨杨仪，今愿助将军攻之。”延大喜，即拔寨引本部兵望南而行。\n" +
                                " \n" +
                                "却说夏侯霸引军至五丈原看时，不见一人，急回报司马懿曰：“蜀兵已尽退矣。”懿跌足曰：“孔明真死矣！可速追之！”夏侯霸曰：“都督不可轻追。当令偏将先往。”懿曰：“此番须吾自行。”遂引兵同二子一齐杀奔五丈原来；呐喊摇旗，杀入蜀寨时，果无一人。懿顾二子曰：“汝急催兵赶来，吾先引军前进。”于是司马师、司马昭在后催军；懿自引军当先，追到山脚下，望见蜀兵不远，乃奋力追赶。忽然山后一声炮响，喊声大震，只见蜀兵俱回旗返鼓，树影中飘出中军大旗，上书一行大字曰：“汉丞相武乡侯诸葛亮”。懿大惊失色。定睛看时，只见中军数十员上将，拥出一辆四轮车来；车上端坐孔明：纶巾羽扇，鹤氅皂绦。懿大惊曰：“孔明尚在！吾轻入重地，堕其计矣！”急勒回马便走。背后姜维大叫：“贼将休走！你中了我丞相之计也！”魏兵魂飞魄散，弃甲丢盔，抛戈撇戟，各逃性命，自相践踏，死者无数。司马懿奔走了五十余里，背后两员魏将赶上，扯住马嚼环叫曰：“都督勿惊。”懿用手摸头曰：“我有头否？”二将曰：“都督休怕，蜀兵去远了。”懿喘息半晌，神色方定；睁目视之，乃夏侯霸、夏侯惠也；乃徐徐按辔，与二将寻小路奔归本寨，使众将引兵四散哨探。\n" +
                                " \n" +
                                "过了两日，乡民奔告曰：“蜀兵退入谷中之时，哀声震地，军中扬起白旗：孔明果然死了，止留姜维引一千兵断后。前日车上之孔明，乃木人也。”懿叹曰：“吾能料其生，不能料其死也！”因此蜀中人谚曰：“死诸葛能走生仲达。”后人有诗叹曰：\n" +
                                " \n" +
                                "长星半夜落天枢，奔走还疑亮未殂。关外至今人冷笑，头颅犹问有和无！\n" +
                                " \n" +
                                "司马懿知孔明死信已确，乃复引兵追赶。行到赤岸坡，见蜀兵已去远，乃引还，顾谓众将曰：“孔明已死，我等皆高枕无忧矣！”遂班师回。一路上见孔明安营下寨之处，前后左右，整整有法，懿叹曰：“此天下奇才也！”于是引兵回长安，分调众将，各守隘口，懿自回洛阳面君去了。\n" +
                                " \n" +
                                "却说杨仪、姜维排成阵势，缓缓退入栈阁道口，然后更衣发丧，扬幡举哀。蜀军皆撞跌而哭，至有哭死者。蜀兵前队正回到栈阁道口，忽见前面火光冲天，喊声震地，一彪军拦路。众将大惊，急报杨仪。正是：\n" +
                                " \n" +
                                "已见魏营诸将去，不知蜀地甚兵来。" +
                                " \n" +
                                "第一百一十九回 假投降巧计成虚话 再受禅依样画" +
                                " \n" +
                                "却说钟会请姜维计议收邓艾之策。维曰：“可先令监军卫瓘收艾。艾若杀瓘，反情实矣。将军却起兵讨之，可也。”会大喜，遂令卫瓘引数十人入成都，收邓艾父子。瓘手下人止之曰：“此是钟司徒令邓征西杀将军，以正反情也。切不可行。”瓘曰：“吾自有计。”遂先发檄文二三十道。其檄曰：“奉诏收艾，其余各无所问。若早来归，爵赏如先，敢有不出者，灭三族。”随备槛车两乘，星夜望成都而来。\n" +
                                " \n" +
                                "比及鸡鸣，艾部将见檄文者，皆来投拜于卫瓘马前。时邓艾在府中未起。瓘引数十人突入大呼曰：“奉诏收邓艾父子！”艾大惊，滚下床来。瓘叱武士缚于车上。其子邓忠出问，亦被捉下，缚于车上。府中将吏大惊，欲待动手抢夺，早望见尘头大起，哨马报说钟司徒大兵到了。众各四散奔走。钟会与姜维下马入府，见邓艾父子已被缚，会以鞭挞邓艾之首而骂曰：“养犊小儿，何敢如此！”姜维亦骂曰：“匹夫行险徼幸，亦有今日耶！”艾亦大骂。会将艾父子送赴洛阳。会入成都，尽得邓艾军马，威声大震。乃谓姜维曰：“吾今日方趁平生之愿矣！”维曰：“昔韩信不听蒯通之说，而有未央宫之祸；大夫种不从范蠡于五湖，卒伏剑而死：斯二子者，其功名岂不赫然哉，徒以利害未明，而见机之不早也。今公大勋已就，威震其主，何不泛舟绝迹，登峨嵋之岭，而从赤松子游乎？”会笑曰：“君言差矣。吾年未四旬，方思进取，岂能便效此退闲之事？”维曰：“若不退闲，当早图良策。此则明公智力所能，无烦老夫之言矣。”会抚掌大笑曰：“伯约知吾心也。”二人自此每日商议大事。维密与后主书曰：“望陛下忍数日之辱，维将使社稷危而复安，日月幽而复明。必不使汉室终灭也。”\n" +
                                " \n" +
                                "却说钟会正与姜维谋反，忽报司马昭有书到。会接书。书中言：“吾恐司徒收艾不下，自屯兵于长安；相见在近，以此先报。”会大惊曰：“吾兵多艾数倍，若但要我擒艾，晋公知吾独能办之。今日自引兵来，是疑我也！”遂与姜维计议。维曰：“君疑臣则臣必死，岂不见邓艾乎？”会曰：“吾意决矣！事成则得天下，不成则退西蜀，亦不失作刘备也。”维曰：“近闻郭太后新亡，可诈称太后有遗诏，教讨司马昭，以正弑君之罪。据明公之才，中原可席卷而定。”会曰：“伯约当作先锋。成事之后，同享富贵。”维曰：“愿效犬马微劳，但恐诸将不服耳。”会曰：“来日元宵佳节，于故宫大张灯火，请诸将饮宴。如不从者尽杀之。”维暗喜。次日，会、维二人请诸将饮宴。数巡后，会执杯大哭。诸将惊问其故，会曰：“郭太后临崩有遗诏在此，为司马昭南阙弑君，大逆无道，早晚将篡魏，命吾讨之。汝等各自佥名，共成此事。”众皆大惊，面面相觑。会拔剑出鞘曰：“违令者斩！”众皆恐惧，只得相从。画字已毕，会乃困诸将于宫中，严兵禁守。维曰：“我见诸将不服，请坑之。”会曰：“吾已令宫中掘一坑，置大棒数千；如不从者，打死坑之。”\n" +
                                " \n" +
                                "时有心腹将丘建在侧。建乃护军胡烈部下旧人也，时胡烈亦被监在宫。建乃密将钟会所言，报知胡烈。烈大惊，泣告曰：“吾儿胡渊领兵在外，安知会怀此心耶？汝可念何日之情，透一消息，虽死无恨。”建曰：“恩主勿忧，容某图之。”遂出告会曰：“主公软监诸将在内，水食不便，可令一人往来传递。”会素听丘建之言，遂令丘建监临。会分付曰：“吾以重事托汝，休得泄漏。”建曰：“主公放心，某自有紧严之法。”建暗令胡烈亲信人入内，烈以密书付其人。其人持书火速至胡渊营内，细言其事，呈上密书。渊大惊，遂遍示诸营知之。众将大怒，急来渊营商议曰：“我等虽死，岂肯从反臣耶？”渊曰：“正月十八日中，可骤入内，如此行之。”监军卫瓘深喜胡渊之谋，即整顿了人马，令丘建传与胡烈。烈报知诸将。\n" +
                                " \n" +
                                "却说钟会请姜维问曰：“吾夜梦大蛇数千条咬吾，主何吉凶？”维曰：“梦龙蛇者，皆吉庆之兆也。”会喜，信其言，乃谓维曰：“器伏已备，放诸将出问之，若何？”维曰：“此辈皆有不服之心，久必为害，不如乘早戮之。”会从之，即命姜维领武士往杀众魏将。维领命，方欲行动，忽然一阵心疼，昏倒在地；左右扶起，半晌方苏。忽报宫外人声。会方令人探时，喊声大震，四面八方，无限兵到。维曰：“此必是诸将作恶，可先斩之。”忽报兵已入内。会令闭上殿门，使军士上殿屋以瓦击之，互相杀死数十人。宫外四面火起，外兵砍开殿门杀入。会自掣剑立杀数人，却被乱箭射倒。众将枭其首。维拔剑上殿，往来冲突，不幸心疼转加。维仰天大叫曰：“吾计不成，乃天命也！”遂自刎而死。时年五十九岁。宫中死者数百人。卫瓘曰：“众军各归营所，以待王命。”魏兵争欲报仇，共剖维腹，其胆大如鸡卵。众将又尽取姜维家属杀之。邓艾部下之人，见钟会、姜维已死，遂连夜去追劫邓艾。早有人报知卫瓘。瓘曰：“是我捉艾；今若留他，我无葬身之地矣。”护军田续曰：“昔邓艾取江油之时，欲杀续，得众官告免。今日当报此恨！”瓘大喜，遂遣田续引五百兵赶至绵竹，正遇邓艾父子放出槛车，欲还成都。艾只道是本部兵到，不作准备；欲待问时，被田续一刀斩之。邓忠亦死于乱军之中。后人有诗叹邓艾曰：\n" +
                                " \n" +
                                "自幼能筹画，多谋善用兵。凝眸知地理，仰面识天文。\n" +
                                " \n" +
                                "马到山根断，兵来石径分。功成身被害，魂绕汉江云。\n" +
                                " \n" +
                                "又有诗叹钟会曰：\n" +
                                " \n" +
                                "髫年称早慧，曾作秘书郎。妙计倾司马，当时号子房。\n" +
                                " \n" +
                                "寿春多赞画，剑阁显鹰扬。不学陶朱隐，游魂悲故乡。\n" +
                                " \n" +
                                "又有诗叹姜维曰：\n" +
                                " \n" +
                                "天水夸英俊，凉州产异才。系从尚父出，术奉武侯来。\n" +
                                " \n" +
                                "大胆应无惧，雄心誓不回。成都身死日，汉将有余哀。\n" +
                                " \n" +
                                "却说姜维、钟会、邓艾已死，张翼等亦死于乱军之中。太子刘璇、汉寿亭侯关彝，皆被魏兵所杀。军民大乱，互相践踏，死者不计其数。旬日后，贾充先至，出榜安民。方始宁靖。留卫瓘守成都，乃迁后主赴洛阳。止有尚书令樊建、侍中张绍、光禄大夫谯周、秘书郎郤正等数人跟随。廖化、董厥皆托病不起，后皆忧死。\n" +
                                " \n" +
                                "时魏景元五年改为咸熙元年，春三月，吴将丁奉见蜀已亡，遂收兵还吴。中书丞华覈奏吴主孙休曰：“吴、蜀乃唇齿也，唇亡则齿寒；臣料司马昭伐吴在即，乞陛下深加防御。”休从其言，遂命陆逊子陆抗为镇东大将军，领荆州牧，守江口；左将军孙异守南徐诸处隘口；又沿江一带，屯兵数百营，老将丁奉总督之，以防魏兵。\n" +
                                " \n" +
                                "建宁太守霍戈闻成都不守，素服望西大哭三日。诸将皆曰：“既汉主失位，何不速降，戈泣谓曰：“道路隔绝，未知吾主安危若何。若魏主以礼待之，则举城而降，未为晚也；万一危辱吾主，则主辱臣死，何可降乎？”众然其言，乃使人到洛阳，探听后主消息去了。\n" +
                                " \n" +
                                "且说后主至洛阳时，司马昭已自回朝。昭责后主曰：“公荒淫无道，废贤失政，理宜诛戮。”后主面如土色，不知所为。文武皆奏曰：“蜀主既失国纪，幸早归降，宜赦之。”昭乃封禅为安乐公，赐住宅，月给用度，赐绢万匹，僮婢百人。子刘瑶及群臣樊建、谯周、郤正等，皆封侯爵。后主谢恩出内。昭因黄皓蠹国害民，令武士押出市曹，凌迟处死。时霍戈探听得后主受封，遂率部下军士来降。次日，后主亲诣司马昭府下拜谢。昭设宴款待，先以魏乐舞戏于前，蜀官感伤，独后主有喜色。昭令蜀人扮蜀乐于前，蜀官尽皆堕泪，后主嬉笑自若。酒至半酣，昭谓贾充曰：“人之无情，乃至于此！虽使诸葛孔明在，亦不能辅之久全，何况姜维乎？”乃问后主曰：“颇思蜀否？”后主曰：“此间乐，不思蜀也。”须臾，后主起身更衣，郤正跟至厢下曰：“陛下如何答应不思蜀也？徜彼再问，可泣而答曰：先人坟墓，远在蜀地，乃心西悲，无日不思。晋公必放陛下归蜀矣。”后主牢记入席。酒将微醉，昭又问曰：“颇思蜀否？”后主如郤正之言以对，欲哭无泪，遂闭其目。昭曰：“何乃似郤正语耶？”后主开目惊视曰：“诚如尊命。”昭及左右皆笑之。昭因此深喜后主诚实，并不疑虑。后人有诗叹曰：\n" +
                                " \n" +
                                "追欢作乐笑颜开，不念危亡半点哀。快乐异乡忘故国，方知后主是庸才。\n" +
                                " \n" +
                                "却说朝中大臣因昭收川有功，遂尊之为王，表奏魏主曹奂。时奂名为天子，实不能主张，政皆由司马氏，不敢不从，遂封晋公司马昭为晋王，谥父司马懿为宣王，兄司马师为景王。昭妻乃王肃之女，生二子：长曰司马炎，人物魁伟，立发垂地，两手过膝，聪明英武，胆量过人；次曰司马攸，情性温和，恭俭孝悌，昭甚爱之，因司马师无子，嗣攸以继其后。昭常曰：“天下者，乃吾兄之天下也。”于是司马昭受封晋王，欲立攸为世子。山涛谏曰：“废长立幼，违礼不祥。”贾充、何曾、裴秀亦谏曰：“长子聪明神武，有超世之才；人望既茂，天表如此：非人臣之相也。”昭犹豫未决。太尉王祥、司空荀顗谏曰：“前代立少，多致乱国。愿殿下思之。”昭遂立长子司马炎为世子。\n" +
                                " \n" +
                                "大臣奏称：“当年襄武县，天降一人，身长二丈余，脚迹长三尺二寸，白发苍髯，着黄单衣；裹黄巾，挂藜头杖，自称曰：吾乃民王也。今来报汝：天下换主，立见太平。如此在市游行三日，忽然不见。此乃殿下之瑞也。殿下可戴十二旒冠冕，建天子旌旗，出警入跸，乘金根车，备六马，进王妃为王后，立世子为太子。”昭心中暗喜；回到宫中，正欲饮食，忽中风不语。次日，病危，太尉王祥、司徒何曾、司马荀顗及诸大臣入宫问安，昭不能言，以手指太子司马炎而死。时八月辛卯日也。何曾曰：“天下大事，皆在晋王；可立太子为晋王，然后祭葬。”是日，司马炎即晋王位，封何曾为晋丞相，司马望为司徒，石苞为骠骑将军，陈骞为车骑将军，谥父为文王。\n" +
                                " \n" +
                                "安葬已毕，炎召贾充、裴秀入宫问曰：“曹操曾云：若天命在吾，吾其为周文王乎！果有此事否？”充曰：“操世受汉禄，恐人议论篡逆之名，故出此言。乃明教曹丕为天子也。”炎曰：“孤父王比曹操何如？”充曰：“操虽功盖华夏，下民畏其威而不怀其德。子丕继业，差役甚重，东西驱驰，未有宁岁。后我宣王、景王，累建大功，布恩施德，天下归心久矣。文王并吞西蜀，功盖寰宇。又岂操之可比乎？”炎曰：“曹丕尚绍汉统，孤岂不可绍魏统耶？”贾充、裴秀二人再拜而奏曰：“殿下正当法曹丕绍汉故事，复筑受禅坛，布告天下，以即大位。”\n" +
                                " \n" +
                                "炎大喜，次日带剑入内。此时，魏主曹奂连日不曾设朝，心神恍惚，举止失措。炎直入后宫，奂慌下御榻而迎。炎坐毕，问曰：“魏之天下，谁之力也？”奂曰：“皆晋王父祖之赐耳。”炎笑曰：“吾观陛下，文不能论道，武不能经邦。何不让有才德者主之？”奂大惊，口噤不能言。傍有黄门侍郎张节大喝曰：“晋王之言差矣！昔日魏武祖皇帝，东荡西除，南征北讨，非容易得此天下；今天子有德无罪，何故让与人耶？”炎大怒曰：“此社稷乃大汉之社稷也。曹操挟天子以令诸侯，自立魏王，篡夺汉室。吾祖父三世辅魏，得天下者，非曹氏之能，实司马氏之力也：四海咸知。吾今日岂不堪绍魏之天下乎？”节又曰：“欲行此事，是篡国之贼也！”炎大怒曰：“吾与汉家报仇，有何不可！”叱武士将张节乱瓜打死于殿下。奂泣泪跪告。炎起身下殿而去。奂谓贾充、裴秀曰：“事已急矣，如之奈何？”充曰：“天数尽矣，陛下不可逆天，当照汉献帝故事，重修受禅坛，具大礼，禅位与晋王：上合天心，下顺民情，陛下可保无虞矣。”\n" +
                                " \n" +
                                "奂从之，遂令贾充筑受禅坛。以十二月甲子日，奂亲捧传国玺，立于坛上，大会文武。后人有诗叹曰：\n" +
                                " \n" +
                                "魏吞汉室晋吞曹，天运循环不可逃。张节可怜忠国死，一拳怎障泰山高。\n" +
                                " \n" +
                                "请晋王司马炎登坛，授与大礼。奂下坛，具公服立于班首。炎端坐于坛上。贾充、裴秀列于左右，执剑，令曹奂再拜伏地听命。充曰：“自汉建安二十五年，魏受汉禅，已经四十五年矣；今天禄永终，天命在晋。司马氏功德弥隆，极天际地，可即皇帝正位，以绍魏统。封汝为陈留王，出就金墉城居止；当时起程，非宣诏不许入京。”奂泣谢而去。太傅司马孚哭拜于奂前曰：“臣身为魏臣，终不背魏也。”炎见孚如此，封孚为安平王。孚不受而退。是日，文武百官，再拜于坛下，山呼万岁。炎绍魏统，国号大晋，改元为泰始元年，大赦天下。魏遂亡。后人有诗叹曰：\n" +
                                " \n" +
                                "晋国规模如魏王，陈留踪迹似山阳。重行受禅台前事，回首当年止自伤。\n" +
                                " \n" +
                                "晋帝司马炎，追谥司马懿为宣帝，伯父司马师为景帝，父司马昭为文帝，立七庙以光祖宗。那七庙？汉征西将军司马钧，钧生豫章太守司马量，量生颍川太守司马隽，隽生京兆尹司马防，防生宣帝司马懿，懿生景帝司马师、文帝司马昭：是为七庙也。大事已定，每日设朝计议伐吴之策。正是：\n" +
                                " \n" +
                                "汉家城郭已非旧，吴国江山将复更。",
                        Charset.defaultCharset(),
                        destFile,
                        true
                    )
            }
        }.start()
    }

    fun writeToFile(inputStream: InputStream, destFile: File, append: Boolean): Boolean {
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(destFile, append)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return if (outputStream == null) {
            false
        } else writeStream(inputStream, outputStream)
    }

    fun writeToFile(
        content: String,
        charset: Charset = Charset.defaultCharset(),
        destFile: File,
        append: Boolean
    ): Boolean {
        try {
            val data = content.toByteArray(charset)
            return writeToFile(data, destFile, append)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
        }
        return false
    }

    fun writeToFile(data: ByteArray, destFile: File, append: Boolean): Boolean {
        var bufferedOut: BufferedOutputStream? = null
        return try {
            bufferedOut = BufferedOutputStream(FileOutputStream(destFile, append))
            bufferedOut.write(data)
            bufferedOut.flush()
            true
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            false
        } finally {
            quickClose(bufferedOut)
        }
    }

    fun writeStream(inputStream: InputStream, outputStream: OutputStream): Boolean {
        return if (inputStream is FileInputStream && outputStream is FileOutputStream) {
            var fis: FileChannel? = null
            var fos: FileChannel? = null
            try {
                fis = inputStream.channel
                fos = outputStream.channel
                fis.transferTo(0, fis.size(), fos) > 0
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
                false
            } finally {
                quickClose(fis)
                quickClose(fos)
            }
        } else {
            val buf = ByteArray(2048)
            var len: Int
            try {
                while (inputStream.read(buf).also { len = it } != -1) {
                    outputStream.write(buf, 0, len)
                    outputStream.flush()
                }
                true
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.message.toString())
                false
            } finally {
                quickClose(inputStream)
                quickClose(outputStream)
            }
        }
    }

    fun readFileBytes(file: File): ByteArray? {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            return readStreamBytes(fis)
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            quickClose(fis)
        }
        return null
    }

    fun readFileBytes(file: File, position: Int, length: Int): ByteArray? {
        if (position < 0 || length <= 0) {
            return null
        }
        var accessFile: RandomAccessFile? = null
        return try {
            accessFile = RandomAccessFile(file, "r")
            accessFile.seek(0)
            val reads = ByteArray(length)
            accessFile.read(reads)
            reads
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            null
        } finally {
            quickClose(accessFile)
        }
    }

    fun readStreamBytes(inputStream: InputStream): ByteArray? {
        return readStreamBytes(inputStream, -1)
    }

    fun readStreamBytes(inputStream: InputStream?, readCount: Int): ByteArray? {
        if (inputStream == null) {
            return null
        }
        var count = readCount
        try {
            if (count <= 0) {
                count = inputStream.available()
            }
            val buffer = ByteArray(count)
            var temp: Int
            var offset = 0
            var maxTime = 10000
            while (offset < count) {
                if (maxTime < 0) {
                    throw IOException("failed to complete after 10000 reads;")
                }
                temp = inputStream.read(buffer, offset, count - offset)
                if (temp < 0) {
                    break
                }
                offset += temp
                maxTime--
            }
            return buffer
        } catch (e: Exception) {
            Log.e(this.javaClass.name, e.message.toString())
            quickClose(inputStream)
        }
        return null
    }

    fun readAssetString(fileName: String): String {
        val sb = StringBuilder()
        var inputStream: InputStream? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = BaseContentProvider.context().assets.open(fileName)
            bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            do {
                line = bufferedReader.readLine()
                if (line != null) {
                    sb.append(line)
                }
            } while (line != null)
        } catch (e: Exception) {
            e.printStackTrace()
            quickClose(inputStream)
            quickClose(bufferedReader)
        }
        return sb.toString()
    }

    fun readAssetString(context: Context, fileName: String): String {
        val sb = StringBuilder()
        var inputStream: InputStream? = null
        var bufferedReader: BufferedReader? = null
        try {
            inputStream = context.assets.open(fileName)
            bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            do {
                line = bufferedReader.readLine()
                if (line != null) {
                    sb.append(line)
                }
            } while (line != null)
        } catch (e: Exception) {
            e.printStackTrace()
            quickClose(inputStream)
            quickClose(bufferedReader)
        }
        return sb.toString()
    }

    fun quickClose(closeable: Closeable?) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun encodeBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun encodeBinary(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String {
        return binaryToStr(String(bytes, charset))
    }

    fun encodeBytes(bytes: ByteArray, charset: Charset = Charset.defaultCharset()): String {
        return String(bytes, charset)
    }

    fun decodeBase64(content: String): ByteArray? {
        return Base64.decode(content, Base64.DEFAULT)
    }

    fun decodeBinary(content: String, charset: Charset = Charset.defaultCharset()): ByteArray {
        val binaryString = strToBinary(content)
        return decodeString(binaryString, charset)
    }

    fun decodeString(content: String, charset: Charset = Charset.defaultCharset()): ByteArray {
        return content.toByteArray(charset)
    }

    private fun bytesToHexString(src: ByteArray?): String {
        val sb = StringBuilder()
        if (src != null && src.isNotEmpty()) {
            for (b in src) {
                val v = b.toInt() and 0xFF
                val hv = Integer.toHexString(v)
                if (hv.length < 2) {
                    sb.append(0)
                }
                sb.append(hv)
            }
        }
        return sb.toString()
    }

    fun parseHeadCode(headCode: String): String {
        val head = headCode.uppercase(Locale.getDefault())
        return if (head.startsWith("FFD8FF")) {
            "image/jpeg"
        } else if (head.startsWith("89504E")) {
            "image/png"
        } else if (head.startsWith("474946")) {
            "image/gif"
        } else if (head.startsWith("524946")) {
            "image/webp"
        } else if (head.startsWith("49492A00")) {
            "image/tiff"
        } else if (head.startsWith("424D")) {
            "image/bmp"
        } else if (head.startsWith("3C3F786D6C")) {
            "application/xml"
        } else if (head.startsWith("68746D6C3E")) {
            "text/html"
        } else if (head.startsWith("255044462D312E")) {
            "application/pdf"
        } else if (head.startsWith("504B0304")) {
            "application/zip"
        } else if (head.startsWith("52617221")) {
            "application/rar"
        } else if (head.startsWith("57415645")) {
            "audio/x-wav"
        } else if (head.startsWith("41564920")) {
            "video/x-msvideo"
        } else if (head.startsWith("2E524D46")) {
            "application/vnd.rn-realmedia"
        } else if (head.startsWith("000001B")) {
            "video/mpeg"
        } else {
            "*/*"
        }
    }

    fun readFileHeadString(file: File): String {
        val inputStream = try {
            FileInputStream(file)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
        val bytes = readStreamBytes(inputStream, 16)
        return bytesToHexString(bytes)
    }

    fun readFileHeadString(context: Context, fileUri: Uri): String {
        val inputStream = try {
            context.contentResolver.openInputStream(fileUri)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
        val bytes = readStreamBytes(inputStream, 16)
        return bytesToHexString(bytes)
    }

    fun getFileTypeCode(file: File): String {
        return parseHeadCode(readFileHeadString(file))
    }

    fun getFileTypeCode(context: Context, fileUri: Uri): String {
        return parseHeadCode(readFileHeadString(context, fileUri))
    }

    fun getFileMimeType(context: Context, fileUri: Uri): String {
        var mimeType: String? = null
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(context, fileUri)
            mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        val resolver = context.contentResolver
        try {
            mimeType = resolver.getType(fileUri)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        try {
            mimeType = parseHeadCode(readFileHeadString(context, fileUri))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mimeType ?: "*/*"
    }

    fun getFileMimeType(file: File): String {
        if (!file.isFile) {
            return "*/*"
        }
        var mimeType: String? = null
        val filePath = file.absolutePath
        val suffix = MimeTypeMap.getFileExtensionFromUrl(filePath)
        try {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(filePath)
            mimeType = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (mimeType != null) {
            return mimeType
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                mimeType = Files.probeContentType(file.toPath())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (mimeType != null) {
            return mimeType
        }
        try {
            mimeType = getFileTypeCode(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mimeType ?: "*/*"
    }

    fun getAvailableStorage(): Long {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

}