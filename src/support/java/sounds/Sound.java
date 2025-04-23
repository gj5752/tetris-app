/*
https://nompor.com/2017/12/14/post-128/
を参考に作成

音声ファイルは
https://fc.sitefactory.info/bgm.html
より
*/

package sounds;

import java.io.*;
import javax.sound.sampled.*;
 
public class Sound {
	
	private Clip clip;


	// インスタンス生成のときにデータを読み込み、再生可能状態にする
	public Sound(InputStream inputStream) throws Exception {
		AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
		this.clip = (Clip)AudioSystem.getLine(new Line.Info(Clip.class));
		this.clip.open(ais);
	}

	// 再生メソッド ループバージョン
	public boolean playloop() {
		// ループ
		this.clip.loop(Clip.LOOP_CONTINUOUSLY);
		return true;
	}

	// 再生メソッド
	public boolean play() {
		this.clip.start();
		return true;
	}

	// 停止メソッド
	public boolean stop() {
		this.clip.stop();
		this.clip.flush();
		return true;
	}

	// 再開メソッド
	public boolean replay() {
		this.clip.stop();
		this.clip.flush();
		this.clip.setFramePosition(0);
		this.clip.start();
		return true;
	}

	// 破棄メソッド
	public boolean close() {
		this.clip.close();
		return true;
	}
 
}