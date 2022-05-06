package installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import org.bspfsystems.simplejson.JSONObject;
import org.bspfsystems.simplejson.parser.JSONException;
import org.bspfsystems.simplejson.parser.JSONParser;

/**
 * Quick and dirty tas installer for minecraft launcher
 * 
 * @author Pancake
 */
public class Installer {

	/**
	 * Text, Progress, Details Gui for debugging the installation process
	 * @author Pancake
	 */
	public static class InstallerWindow extends JFrame {
		
		final JProgressBar progressBar;
		final JTextArea txtrStart;
		
		public InstallerWindow(long len) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
			super("Minecraft TAS");
			
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			
			this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			this.setBounds(100, 100, 700, 400);
			
			final JPanel contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			setContentPane(contentPane);
			
			final JLabel lblNewLabel = new JLabel("Installing Minecraft TAS...");
			lblNewLabel.setFont(UIManager.getFont("TextArea.font"));
			lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
			lblNewLabel.setBounds(0, 0, 684, 40);
			contentPane.add(lblNewLabel);
			
			this.progressBar = new JProgressBar();
			this.progressBar.setMaximum((int) (len/1000));
			this.progressBar.setValue(0);
			this.progressBar.setBounds(10, 38, 664, 20);
			contentPane.add(this.progressBar);
			
			this.txtrStart = new JTextArea();
			this.txtrStart.setEditable(false);
			this.txtrStart.setBounds(10, 69, 664, 281);
			this.txtrStart.setLineWrap(true);
			contentPane.add(this.txtrStart);
			
			this.setVisible(true);
		}
		
		public void update(int p) {
			this.progressBar.setValue(this.progressBar.getValue() + p);
		}
		
		public void print(String p) {
			this.txtrStart.setText(p + "\n" + this.txtrStart.getText());
		}
		
	}
	
	public static void main(String[] args) throws IOException, JSONException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
		final String APPDATA = System.getenv("AppData");
		final String MINECRAFT = APPDATA + "\\.minecraft";
		final File LAUNCHER_PROFILES = new File(MINECRAFT, "launcher_profiles.json");
		final File LAUNCHER_PROFILES_OLD = new File(MINECRAFT, "launcher_profiles_OLD.json");
		final File LAUNCHER_PROFILES_NEW = new File(MINECRAFT, "launcher_profiles_NEW.json");
		
		// Ask user for confirmation
		long len = Long.valueOf(new BufferedReader(new InputStreamReader(new URL("https://mgnet.work/mclauncher-installer.len").openStream())).readLine());
		long size = (long) (len / 1e+6);
		if (JOptionPane.showConfirmDialog(null, "Installing Minecraft TAS will download " + size + " MB.", "Minecraft TAS", JOptionPane.OK_CANCEL_OPTION) != 0)
			return;
		
		InstallerWindow w = new InstallerWindow(len);
		
		// Extract .minecraft package
		ZipInputStream zis = new ZipInputStream(new URL("https://mgnet.work/mclauncher-installer.zip").openStream());
		ZipEntry e;
		while ((e = zis.getNextEntry()) != null) {
			File f = new File(MINECRAFT, e.getName());
			if (e.isDirectory())
				f.mkdirs();
			else {
				Files.copy(zis, f.toPath(), StandardCopyOption.REPLACE_EXISTING);
				w.update((int) (e.getCompressedSize()/1000));
				w.print("Downloaded " + e.getName() + " (" + (e.getSize()/1e+6) + "MB)");
			}
		}
		zis.closeEntry();
		zis.close();
		
		// Update launcher_profiles.json
		LAUNCHER_PROFILES.renameTo(LAUNCHER_PROFILES_OLD);
		JSONObject new_profiles = JSONParser.deserializeObject(new String(Files.readAllBytes(LAUNCHER_PROFILES_NEW.toPath()))).getObject("profiles");
		JSONObject json = JSONParser.deserializeObject(new String(Files.readAllBytes(LAUNCHER_PROFILES_OLD.toPath())));
		JSONObject profiles = json.getObject("profiles");
		for (Entry<String, Object> entry : new_profiles)
			profiles.set(entry.getKey(), entry.getValue());
		json.set("profiles", profiles);
		
		// Write launcher_profiles.json
		Files.write(LAUNCHER_PROFILES.toPath(), JSONParser.serialize(json).replace("%APPDATA%", APPDATA.replace("\\", "\\\\")).getBytes(), StandardOpenOption.CREATE);
		
		JOptionPane.showMessageDialog(null, "Minecraft TAS was installed successfully", "Minecraft TAS", JOptionPane.INFORMATION_MESSAGE);
		System.exit(0);
	}

}
