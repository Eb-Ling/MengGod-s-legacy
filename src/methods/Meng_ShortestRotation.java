package data.methods;

public class Meng_ShortestRotation {
    public static float findShortestRotation(float arg1,float arg2){
        float rotation;
        while (arg1>360f){
            arg1-=360f;
        }
        while (arg1<0f){
            arg1+=360f;
        }
        if (Math.abs(arg1 - arg2) <= 180f) {
            if (arg2 <= arg1) {
                rotation = 1f;
            } else {
                rotation = -1f;
            }
        } else {
            if (arg2 <= arg1) {
                rotation = -1f;
            } else {
                rotation = 1f;
            }

        }
        return rotation;
    }
}
