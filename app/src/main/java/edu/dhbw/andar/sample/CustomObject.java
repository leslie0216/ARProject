package edu.dhbw.andar.sample;

import android.content.Context;

import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import edu.dhbw.andar.ARObject;
import edu.dhbw.andar.pub.SimpleBox;
import edu.dhbw.andar.util.GraphicsUtil;

/**
 * An example of an AR object being drawn on a marker.
 * @author tobi
 *
 */
public class CustomObject extends ARObject {

    Context m_context;

	
	public CustomObject(String name, String patternName,
			double markerWidth, double[] markerCenter) {
		super(name, patternName, markerWidth, markerCenter);
		float   mat_ambientf[]     = {0f, 1.0f, 0f, 1.0f};
		float   mat_flashf[]       = {0f, 1.0f, 0f, 1.0f};
		float   mat_diffusef[]       = {0f, 1.0f, 0f, 1.0f};
		float   mat_flash_shinyf[] = {50.0f};

		mat_ambient = GraphicsUtil.makeFloatBuffer(mat_ambientf);
		mat_flash = GraphicsUtil.makeFloatBuffer(mat_flashf);
		mat_flash_shiny = GraphicsUtil.makeFloatBuffer(mat_flash_shinyf);
		mat_diffuse = GraphicsUtil.makeFloatBuffer(mat_diffusef);
		
	}
	public CustomObject(String name, String patternName,
			double markerWidth, double[] markerCenter, float[] customColor) {
		super(name, patternName, markerWidth, markerCenter);
		float   mat_flash_shinyf[] = {50.0f};

		mat_ambient = GraphicsUtil.makeFloatBuffer(customColor);
		mat_flash = GraphicsUtil.makeFloatBuffer(customColor);
		mat_flash_shiny = GraphicsUtil.makeFloatBuffer(mat_flash_shinyf);
		mat_diffuse = GraphicsUtil.makeFloatBuffer(customColor);
		
	}
	
	/**
	 * Just a box, imported from the AndAR project.
	 */
	private SimpleBox box = new SimpleBox();
	private FloatBuffer mat_flash;
	private FloatBuffer mat_ambient;
	private FloatBuffer mat_flash_shiny;
	private FloatBuffer mat_diffuse;
	
	/**
	 * Everything drawn here will be drawn directly onto the marker,
	 * as the corresponding translation matrix will already be applied.
	 */
	@Override
	public final void draw(GL10 gl) {
		super.draw(gl);
		
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR,mat_flash);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, mat_flash_shiny);	
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, mat_diffuse);	
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, mat_ambient);

	    //draw cube
	    gl.glColor4f(0, 1.0f, 0, 1.0f);
	    gl.glTranslatef( 0.0f, 0.0f, 12.5f );
	    
	    //draw the box
	    box.draw(gl);

        updateView();
	}
	@Override
	public void init(GL10 gl) {
		
	}

    public void setContext(Context context)
    {
        m_context = context;
    }

    public void updateView()
    {
        //CustomActivity ca = (CustomActivity)m_context;
        float[] mat = getGlMatrix();

        float[][] Mr = {{mat[0], mat[4], mat[8]}, {mat[1], mat[5], mat[9]}, {mat[2], mat[6], mat[10]}};

        float[] Vt = {mat[12], mat[13], mat[14]};

        float Tx = Mr[0][0] * Vt[0] + Mr[0][1] * Vt[1] + Mr[0][2] * Vt[2];
        float Ty = Mr[1][0] * Vt[0] + Mr[1][1] * Vt[1] + Mr[1][2] * Vt[2];
        float Tz = Mr[2][0] * Vt[0] + Mr[2][1] * Vt[1] + Mr[2][2] * Vt[2];

        ((CustomActivity)m_context).updateCoordinates(Tx, Ty, Tz);
        ((CustomActivity)m_context).updateglmatirx(mat);
        //((CustomActivity)m_context).updateCoordinates(mat[12], mat[13], mat[14]);
        /*
        System.out.print(Tx);
        System.out.print(" ");
        System.out.print(Ty);
        System.out.print(" ");
        System.out.print(Tz);
        System.out.print(" ");
        System.out.println();
        */

        //((CustomActivity)m_context).updateCordinates((float)mat[12], (float)mat[13], (float)mat[14]);
        /*
        int i =0;
        for (; i < 16; ++i) {
            //if (i == 3 || i == 7 || i == 11true) {
                System.out.print(mat[i]);
                System.out.print(" ");
            //}
        }
        System.out.println();
        */


        /*
        ca.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double[] mat = getTransMatrix();

                ((CustomActivity)m_context).updateDrawView();
            }
        });
        */
    }
}
