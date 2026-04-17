package com.glsl.shader.render;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import com.glsl.shader.render.databinding.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class MainActivity extends Activity {
	
	private MainBinding binding;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		binding = MainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		initialize(_savedInstanceState);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
	}
	
	private void initializeLogic() {
		// ===== RENDERER =====
		class MyRenderer implements android.opengl.GLSurfaceView.Renderer {
			
			private android.content.Context context;
			private int program;
			private int textureId;
			private float time = 0;
			
			public int effectType = 2;
			
			public MyRenderer(android.content.Context ctx) {
				context = ctx;
			}
			
			// ===== VERTEX SHADER =====
			private final String vertexShaderCode =
			"attribute vec4 aPosition;" +
			"attribute vec2 aTexCoord;" +
			"varying vec2 vTexCoord;" +
			"void main() {" +
			"  gl_Position = aPosition;" +
			"  vTexCoord = aTexCoord;" +
			"}";
			
			// ===== FRAGMENT SHADER =====
			private final String fragmentShaderCode =
			"precision mediump float;" +
			
			"uniform sampler2D uTexture;" +
			"uniform float uTime;" +
			"uniform float uSpeed;" +
			"uniform float uFrequency;" +
			"uniform float uWaveAmplitude;" +
			"uniform int effectType;" +
			
			"varying vec2 vTexCoord;" +
			
			"const int EFFECT_TYPE_FLAG = 0;" +
			"const int EFFECT_TYPE_DREAMY = 1;" +
			"const int EFFECT_TYPE_WAVY = 2;" +
			"const int EFFECT_TYPE_HEAT_WAVE_HORIZONTAL = 3;" +
			"const int EFFECT_TYPE_HEAT_WAVE_VERTICAL = 4;" +
			
			"vec2 sineWave(vec2 pt) {" +
			"  float x = 0.0;" +
			"  float y = 0.0;" +
			
			"  if (effectType == EFFECT_TYPE_DREAMY) {" +
			"    float offsetX = sin(pt.y * uFrequency + uTime * uSpeed) * uWaveAmplitude;" +
			"    pt.x += offsetX;" +
			"  }" +
			
			"  else if (effectType == EFFECT_TYPE_WAVY) {" +
			"    float offsetY = sin(pt.x * uFrequency + uTime * uSpeed) * uWaveAmplitude;" +
			"    pt.y += offsetY;" +
			"  }" +
			
			"  else if (effectType == EFFECT_TYPE_HEAT_WAVE_HORIZONTAL) {" +
			"    x = sin(pt.x * uFrequency + uTime * uSpeed) * uWaveAmplitude;" +
			"  }" +
			
			"  else if (effectType == EFFECT_TYPE_HEAT_WAVE_VERTICAL) {" +
			"    y = sin(pt.y * uFrequency + uTime * uSpeed) * uWaveAmplitude;" +
			"  }" +
			
			"  else if (effectType == EFFECT_TYPE_FLAG) {" +
			"    y = sin(pt.y * uFrequency + 10.0 * pt.x + uTime * uSpeed) * uWaveAmplitude;" +
			"    x = sin(pt.x * uFrequency + 5.0 * pt.y + uTime * uSpeed) * uWaveAmplitude;" +
			"  }" +
			
			"  return vec2(pt.x + x, pt.y + y);" +
			"}" +
			
			"void main() {" +
			"  vec2 uv = sineWave(vTexCoord);" +
			"  uv = clamp(uv, 0.0, 1.0);" +
			"  gl_FragColor = texture2D(uTexture, uv);" +
			"}";
			
			private int loadShader(int type, String code) {
				int shader = android.opengl.GLES20.glCreateShader(type);
				android.opengl.GLES20.glShaderSource(shader, code);
				android.opengl.GLES20.glCompileShader(shader);
				return shader;
			}
			
			private float[] vertices = {
				-1f,  1f,   0f, 0f,
				-1f, -1f,   0f, 1f,
				1f,  1f,   1f, 0f,
				1f, -1f,   1f, 1f
			};
			
			private java.nio.FloatBuffer buffer;
			
			@Override
			public void onSurfaceCreated(javax.microedition.khronos.opengles.GL10 gl,
			javax.microedition.khronos.egl.EGLConfig config) {
				
				int vShader = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShaderCode);
				int fShader = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
				
				program = android.opengl.GLES20.glCreateProgram();
				android.opengl.GLES20.glAttachShader(program, vShader);
				android.opengl.GLES20.glAttachShader(program, fShader);
				android.opengl.GLES20.glLinkProgram(program);
				
				java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(vertices.length * 4);
				bb.order(java.nio.ByteOrder.nativeOrder());
				buffer = bb.asFloatBuffer();
				buffer.put(vertices);
				buffer.position(0);
				
				try {
					String[] files = context.getAssets().list("");
					
					java.util.ArrayList<String> imagens = new java.util.ArrayList<>();
					
					for (String file : files) {
						if (file.toLowerCase().endsWith(".png") || file.toLowerCase().endsWith(".jpg")) {
							imagens.add(file);
						}
					}
					
					if (!imagens.isEmpty()) {
						int index = new java.util.Random().nextInt(imagens.size());
						textureId = loadTexture(context, imagens.get(index));
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				android.opengl.GLES20.glClearColor(0, 0, 0, 1);
			}
			
			@Override
			public void onDrawFrame(javax.microedition.khronos.opengles.GL10 gl) {
				android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT);
				android.opengl.GLES20.glUseProgram(program);
				
				int posHandle = android.opengl.GLES20.glGetAttribLocation(program, "aPosition");
				int texHandle = android.opengl.GLES20.glGetAttribLocation(program, "aTexCoord");
				
				int timeHandle = android.opengl.GLES20.glGetUniformLocation(program, "uTime");
				int speedHandle = android.opengl.GLES20.glGetUniformLocation(program, "uSpeed");
				int freqHandle = android.opengl.GLES20.glGetUniformLocation(program, "uFrequency");
				int ampHandle = android.opengl.GLES20.glGetUniformLocation(program, "uWaveAmplitude");
				int effectHandle = android.opengl.GLES20.glGetUniformLocation(program, "effectType");
				
				buffer.position(0);
				android.opengl.GLES20.glVertexAttribPointer(posHandle, 2,
				android.opengl.GLES20.GL_FLOAT, false, 16, buffer);
				android.opengl.GLES20.glEnableVertexAttribArray(posHandle);
				
				buffer.position(2);
				android.opengl.GLES20.glVertexAttribPointer(texHandle, 2,
				android.opengl.GLES20.GL_FLOAT, false, 16, buffer);
				android.opengl.GLES20.glEnableVertexAttribArray(texHandle);
				
				time += 0.05f;
				
				android.opengl.GLES20.glUniform1f(timeHandle, time);
				android.opengl.GLES20.glUniform1f(speedHandle, 0.4f);
				android.opengl.GLES20.glUniform1f(freqHandle, 5.0f);
				android.opengl.GLES20.glUniform1f(ampHandle, 0.1f);
				android.opengl.GLES20.glUniform1i(effectHandle, effectType);
				
				android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0);
				android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, textureId);
				
				int texUniform = android.opengl.GLES20.glGetUniformLocation(program, "uTexture");
				android.opengl.GLES20.glUniform1i(texUniform, 0);
				
				android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4);
			}
			
			@Override
			public void onSurfaceChanged(javax.microedition.khronos.opengles.GL10 gl,
			int width, int height) {
				android.opengl.GLES20.glViewport(0, 0, width, height);
			}
			
			public void nextEffect() {
				effectType++;
				if (effectType > 4) effectType = 0;
			}
			
			private int loadTexture(android.content.Context context, String fileName) {
				int[] texture = new int[1];
				android.opengl.GLES20.glGenTextures(1, texture, 0);
				
				android.opengl.GLES20.glBindTexture(android.opengl.GLES20.GL_TEXTURE_2D, texture[0]);
				
				android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D,
				android.opengl.GLES20.GL_TEXTURE_MIN_FILTER,
				android.opengl.GLES20.GL_LINEAR);
				
				android.opengl.GLES20.glTexParameteri(android.opengl.GLES20.GL_TEXTURE_2D,
				android.opengl.GLES20.GL_TEXTURE_MAG_FILTER,
				android.opengl.GLES20.GL_LINEAR);
				
				
				android.opengl.GLES20.glTexParameteri(
				android.opengl.GLES20.GL_TEXTURE_2D,
				android.opengl.GLES20.GL_TEXTURE_WRAP_S,
				android.opengl.GLES20.GL_CLAMP_TO_EDGE
				);
				
				android.opengl.GLES20.glTexParameteri(
				android.opengl.GLES20.GL_TEXTURE_2D,
				android.opengl.GLES20.GL_TEXTURE_WRAP_T,
				android.opengl.GLES20.GL_CLAMP_TO_EDGE
				);
				
				try {
					java.io.InputStream is = context.getAssets().open(fileName);
					android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
					
					android.opengl.GLUtils.texImage2D(
					android.opengl.GLES20.GL_TEXTURE_2D,
					0,
					bitmap,
					0
					);
					
					bitmap.recycle();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return texture[0];
			}
		}
		
		
		// ===== ACTIVITY =====
		final MyRenderer renderer = new MyRenderer(this);
		
		android.opengl.GLSurfaceView glView = new android.opengl.GLSurfaceView(this);
		glView.setEGLContextClientVersion(2);
		glView.setRenderer(renderer);
		
		glView.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(android.view.View v) {
				renderer.nextEffect();
			}
		});
		
		setContentView(glView);
	}
	
}
