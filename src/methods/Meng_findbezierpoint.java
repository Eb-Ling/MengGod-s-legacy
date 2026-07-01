package data.methods;

import org.lwjgl.util.vector.Vector2f;

public class Meng_findbezierpoint {
    public static Vector2f findpoint(Vector2f p1,Vector2f p2,Vector2f p3,Vector2f p4,float t){
        return new Vector2f(p1.x*(1-t)*(1-t)*(1-t)+3f*p2.x*t*(1-t)*(1-t)+3*p3.x*t*t*(1-t)+p4.x*t*t*t,p1.y*(1-t)*(1-t)*(1-t)+3f*p2.y*t*(1-t)*(1-t)+3f*p3.y*t*t*(1-t)+p4.y*t*t*t);
    }
    public static Vector2f findpointf2(Vector2f p1, Vector2f p2, Vector2f p3, float t){
        return new Vector2f(p1.x*(1-t)*(1-t)+2f*p2.x*t*(1-t)+p3.x*t*t,p1.y*(1-t)*(1-t)+2f*p2.y*t*(1-t)+p3.y*t*t);
    }
}
