package App;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JCheckBox;

public class AudioPlayerFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private Clip clip;
	private FloatControl volumeControl;
	private long clipTimePosition = 0;

	private Timer fadeOutTimer;
	private static final int FADE_OUT_DURATION_MS = 5000;
	private static final int FADE_OUT_INTERVAL_MS = 100;
	private boolean isLooping = false;

	private JPanel contentPane;
	private File currentAudioFile;
	private File nextAudioFile;
	private JLabel lblCurrentSong;
	private JButton btnStop;
	private JButton btnPlay;
	private JButton btnPause;
	private JSlider volumeSlider;
	private JLabel lblSeparator;
	private JLabel lblUpcomingSong;
	private JButton btnNextSong;

	public AudioPlayerFrame() {
		setTitle("Audio Player");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(450, 350);
		setLocationRelativeTo(null);
		setResizable(false);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		btnPlay = new JButton("Play");
		btnPlay.setBounds(32, 200, 53, 23);
		btnPlay.setEnabled(false);
		btnPlay.addActionListener(e -> playAudio());
		contentPane.add(btnPlay);

		btnStop = new JButton("Stop");
		btnStop.setBounds(224, 200, 55, 23);
		btnStop.setEnabled(false);
		btnStop.addActionListener(e -> stopAudio());
		contentPane.add(btnStop);

		lblCurrentSong = new JLabel("");
		lblCurrentSong.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				chooseFile(0, lblCurrentSong);
			}
		});

		lblCurrentSong.setBorder(
				new TitledBorder(null, "Canci\u00F3n Actual", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		lblCurrentSong.setFont(new Font("Tahoma", Font.PLAIN, 10));
		lblCurrentSong.setBounds(73, 85, 113, 45);
		lblCurrentSong.setHorizontalAlignment(SwingConstants.CENTER);
		contentPane.add(lblCurrentSong);

		btnPause = new JButton("Pause");
		btnPause.setBounds(117, 200, 75, 23);
		btnPause.setEnabled(false);
		btnPause.addActionListener(e -> pauseAudio());
		contentPane.add(btnPause);

		volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
		volumeSlider.setBounds(73, 135, 288, 45);
		volumeSlider.setMajorTickSpacing(10);
		volumeSlider.setMinorTickSpacing(1);
		volumeSlider.setPaintTicks(true);
		volumeSlider.setPaintLabels(true);
		volumeSlider.addChangeListener(e -> adjustVolume());
		contentPane.add(volumeSlider);

		lblUpcomingSong = new JLabel("");
		lblUpcomingSong.setBorder(
				new TitledBorder(null, "Siguiente Canci\u00F3n", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		lblUpcomingSong.setHorizontalAlignment(SwingConstants.CENTER);
		lblUpcomingSong.setFont(new Font("Tahoma", Font.PLAIN, 10));
		lblUpcomingSong.setBounds(248, 85, 113, 45);
		lblUpcomingSong.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (chooseFile(1, lblUpcomingSong))
					btnNextSong.setEnabled(true);
			}
		});
		contentPane.add(lblUpcomingSong);

		lblSeparator = new JLabel(">>");
		lblSeparator.setHorizontalAlignment(SwingConstants.CENTER);
		lblSeparator.setBounds(194, 100, 46, 14);
		contentPane.add(lblSeparator);

		btnNextSong = new JButton("Siguiente");
		btnNextSong.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playNextSong();
			}
		});
		btnNextSong.setEnabled(false);
		btnNextSong.setBounds(311, 200, 89, 23);
		contentPane.add(btnNextSong);

		JCheckBox chckbxLoop = new JCheckBox("Bucle");
		chckbxLoop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				isLooping = !isLooping;
			}
		});
		chckbxLoop.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxLoop.setBounds(168, 248, 97, 23);
		contentPane.add(chckbxLoop);
	}

	private boolean chooseFile(int action, JLabel label) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Desktop"));

		FileNameExtensionFilter filter = new FileNameExtensionFilter("Audio Files", "wav");
		chooser.setFileFilter(filter);

		int returnVal = chooser.showOpenDialog(null);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			if (clip != null && clip.isActive()) {
				clip.stop();
			}

			if (action == 0) {
				currentAudioFile = chooser.getSelectedFile();
				label.setText("<html>" + currentAudioFile.getName() + "</html>");
			} else if (action == 1) {
				nextAudioFile = chooser.getSelectedFile();
				label.setText("<html>" + nextAudioFile.getName() + "</html>");
			}

			btnPlay.setEnabled(true);
			btnStop.setEnabled(false);
			btnPause.setEnabled(false);

			return true;
		} else {
			return false;
		}

	}

	private void playAudio() {
		if (currentAudioFile == null) {
			System.err.println("No audio file selected.");
			return;
		}

		try {
			if (clip != null && clip.isRunning()) {
				clip.stop();
			}

			AudioInputStream audioStream = AudioSystem.getAudioInputStream(currentAudioFile);
			clip = AudioSystem.getClip();
			clip.open(audioStream);
			clip.setMicrosecondPosition(clipTimePosition);
			clip.addLineListener(new LineListener() {
				@Override
				public void update(LineEvent event) {
					if (event.getType() == LineEvent.Type.STOP) {
						if (clip == event.getLine()) {
							if (nextAudioFile != null) {
								playNextSong();
							}
						}
					}
				}
			});

			if (isLooping)
				clip.loop(Clip.LOOP_CONTINUOUSLY);

			clip.start();

			// Get the volume control
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			}

			btnPlay.setEnabled(false);
			btnStop.setEnabled(true);
			btnPause.setEnabled(true);

			// Set initial volume based on slider
			adjustVolume();
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	private void stopAudio() {
		if (clip != null && clip.isRunning()) {
			btnStop.setEnabled(false);
			btnPause.setEnabled(false);
			fadeOutAudio();
		}
	}

	private void pauseAudio() {
		if (clip != null && clip.isRunning()) {
			clip.stop();
			clipTimePosition = clip.getMicrosecondPosition();
		}

		btnPlay.setEnabled(true);
		btnStop.setEnabled(true);
		btnPause.setEnabled(false);
	}

	private void playNextSong() {
		if (nextAudioFile != null) {
			currentAudioFile = nextAudioFile;
			nextAudioFile = null;
			lblUpcomingSong.setText("");
			lblCurrentSong.setText("<html>" + currentAudioFile.getName() + "</html>");
			btnNextSong.setEnabled(false);
			playAudio();
		} else {
			System.err.println("Next audio file is null");
		}
	}

	private void adjustVolume() {
		if (volumeControl != null) {
			float value = volumeSlider.getValue() / 100.0f;
			float minGain = volumeControl.getMinimum();
			float maxGain = volumeControl.getMaximum();
			float gain = minGain + (maxGain - minGain) * value;

			volumeControl.setValue(gain);
		}
	}

	private void fadeOutAudio() {
		if (volumeControl == null || clip == null || !clip.isRunning()) {
			return;
		}

		final float startVolume = volumeSlider.getValue() / 100.0f;
		final float minGain = volumeControl.getMinimum();
		final float maxGain = volumeControl.getMaximum();
		final float startGain = minGain + (maxGain - minGain) * startVolume;

		final float fadeStep = (startGain - minGain) / (FADE_OUT_DURATION_MS / FADE_OUT_INTERVAL_MS);

		fadeOutTimer = new Timer(FADE_OUT_INTERVAL_MS, e -> {
			if (volumeControl != null) {
				float currentGain = volumeControl.getValue();
				if (currentGain > minGain) {
					volumeControl.setValue(Math.max(currentGain - fadeStep, minGain));
				} else {
					btnPlay.setEnabled(true);
					btnStop.setEnabled(false);
					btnPause.setEnabled(false);
					clip.stop();
					clipTimePosition = 0;
					fadeOutTimer.stop();
					fadeOutTimer = null;
				}
			} else {
				fadeOutTimer.stop();
			}
		});

		fadeOutTimer.start();
	}

	public static void main(String[] args) {
		FlatLightLaf.setup();

		EventQueue.invokeLater(() -> {
			try {
				AudioPlayerFrame frame = new AudioPlayerFrame();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
