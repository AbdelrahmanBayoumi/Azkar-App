
<div align=right dir=rtl>

## المتطلبات


* الإصدار [8 من Java](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html) (بالنسبة للمطورين)، لاستخدام التطبيق بشكل طبيعي يمكنك تحميله: [ من هنا ⬇️](https://azkar-site.web.app/#download)
- بالإضافة إلى [Maven 3.3+ ](https://maven.apache.org)
    - [(شرح التثبيت)](https://youtu.be/--Iv5vBIHjI)
 
- برنامج [Scene Builder](https://gluonhq.com/products/scene-builder/): هو أداة تصميم واجهات JavaFX بشكل مرئي بدون كتابة أكواد FXML يدويًا.

    <details>
      <summary>تأكد من تحميل الإصدار 8.5.0 ليكون متوافقًا مع Java 8</summary>    
      <img src="https://github.com/user-attachments/assets/b885c8ad-3ed3-4a70-9c4b-213d55252b6e">
    </details><details>
      <summary>أيضًا يجب استيراد مكتبات الواجهة المستخدمة في Scene Builder إذا كنت ستستخدمه</summary>    
      <img src="https://github.com/user-attachments/assets/2a5ac3b3-9661-4a0f-9404-51c929837341">
      <img src="https://github.com/user-attachments/assets/88c8b5d2-78dd-4b19-bf0d-2863f6705edb">
      <img src="https://github.com/user-attachments/assets/e85b661a-02f2-4b2c-ba0c-5b76af9c9109">
    </details>

## تثبيت المشروع للتطوير




1. قم بتنزيل ملفات المستودع (المشروع) من قسم التنزيل أو عمل Clone لهذا المشروع عن طريق كتابة الأمر التالي في bash:
<div align=left>
       
       git clone https://github.com/AbdelrahmanBayoumi/Azkar-App.git

</div>

2. قم باستيراده في Intellij IDEA أو أي Java IDE آخر ودع Maven يقوم بتنزيل التبعيات المطلوبة لك.

3. لاستخدام Sentry (لتقارير الأخطاء) ، تحتاج إلى إضافة DSN الخاص بك في ملف `src/main/resources/sentry.properties`.
    - انظر `sentry.properties.example` لرؤية كيفية القيام بذلك.
  
4. لاستخدام خدمات `ip2location` للموقع، تحتاج إلى إضافة `ip2location.apiKey` الخاص بك في ملف `src/main/resources/config.properties`.
    - انظر `config.properties.example` لرؤية كيفية القيام بذلك.
       
5. قم بتشغيل التطبيق 😁
       
</div>

