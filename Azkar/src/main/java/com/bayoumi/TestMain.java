package com.bayoumi;

public class TestMain {
    public static void main(String[] args) {
        String s = "امتِنَاعُ الشَّفَاعَةِ في الحَدِّ بَعْدَ بُلُوغِهِ السُّلْطان. أَنَّ جَاحِدَ الْعَاريَة حُكْمُهِ حُكْمُ السَّارِقِ، فَيُقْطَع. وجُوبُ الْعَدْلِ والْمُسَاوَاةِ بَيْنَ النَّاسِ، سَوَاءً مِنهم الغِني أو الفَقير، والشَّرِيف أو الوَضِيع، في الأحْكَامِ والحُدُودِ، وفيما هُمْ مُشْتَركُونُ فِيهِ. أَنَّ إِقَامَةَ الحُدُودِ عَلَى الضُّعَفَاء وتَعْطِيلِها في حَقِّ الْأَقْوِيَاء سبب الهلاك والدَّمَار وشَقَاوَة الدَّارَيْن. جَوَازُ الْمُبَالَغَةِ فِي الكلام، والتَّشبيهُ والتَّمْثيل، لِتَوضِيح الحقِّ وتَبيينه وتَأْكِيده. مَنْقَبَةٌ كُبْرَى لِأَسَامة، إذ لم يروا أولى منه للشَّفاعة عند النبي -صلى الله عليه وسلم - وقَدْ وَقَعَتْ الحَادِثَة فِي فَتْحِ مَكَة. عظيم منزلة فاطمة -رضي الله عنها- عند النبي -صلى الله عليه وسلم-. تعظيم أمر المحاباة للأشراف في حقوق الله تعالى. الاعتبار بأحوال من مضى من الأمم ولا سيما من خالف أمر الشارع. دخول النساء مع الرجال في حد السرقة.";
        String[] splitString = s.split("\\.");
        for (String ss : splitString) {
            System.out.println("= [" + ss.trim() + "].\n");
        }
    }
}
