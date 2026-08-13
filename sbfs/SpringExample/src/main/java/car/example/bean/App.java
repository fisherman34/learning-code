package car.example.bean;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
    public static void main(String[] args) {

        /*
         * SpringのApplicationContextを作成する。
         *
         * ClassPathXmlApplicationContextは、
         * クラスパス（src/main/resourcesなど）にある
         * XML設定ファイルを読み込んでSpringコンテナを初期化する。
         *
         * 今回は applicationBeanContext.xml を読み込む。
         */
        ApplicationContext context
                = new ClassPathXmlApplicationContext("applicationBeanContext.xml");

        /*
         * SpringコンテナからBeanを取得する。
         *
         * "myBean" は applicationBeanContext.xml に
         * 定義されているBeanの名前。
         *
         * getBean()の戻り値はObject型なので、
         * MyBean型にキャストしている。
         */
        MyBean myBean = (MyBean) context.getBean("myBean");

        /*
         * myBeanをコンソールに出力する。
         *
         * オブジェクトをprintln()に渡すと、
         * MyBeanクラスで@Overrideした
         * toString()メソッドが自動的に呼び出される。
         */
        System.out.println(myBean);
    }
}
