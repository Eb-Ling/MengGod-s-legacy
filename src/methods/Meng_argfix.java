package data.methods;

public class Meng_argfix {
    public static float fix(float arg){
        while (arg>360f){
            arg-=360f;
        }
        while (arg<0f){
            arg+=360f;
        }
        return arg;
    }
}
