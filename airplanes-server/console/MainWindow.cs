using System;
using System.Collections.Generic;
using System.Threading;
using System.Windows.Forms;
using System.ComponentModel;
using System.IO;

namespace console
{
	public partial class MainWindow : Form
	{
		public MainWindow()
		{
			InitializeComponent();
			fpsInput.ContextMenu = new ContextMenu();
			portInput.ContextMenu = new ContextMenu();
		}

		private void numericFilter_TextChanged(object sender, EventArgs e)
		{
			if (!(sender is TextBox))
				throw new System.ArgumentException("Control type must be TextBox");

			TextBox control = sender as TextBox;
			string text = control.Text;

			try
			{
				Int32 value = Convert.ToInt32(text.Substring(0, Math.Min(text.Length, control.MaxLength)));
				control.Text = value.ToString();
				control.SelectionStart = control.Text.Length;
			}
			catch (System.FormatException) { }
			catch (System.OverflowException) { }
		}

		private void numericFilter_KeyPress(object sender, KeyPressEventArgs e)
		{
			if (!(sender is TextBox))
				throw new System.ArgumentException("Control type must be TextBox");
			TextBox control = sender as TextBox;
			Int32 maxValue;
			try
			{
				maxValue = Convert.ToInt32(control.Tag);
				if (maxValue == 0)
					maxValue = Int32.MaxValue;
			}
			catch (System.FormatException) { throw new ArgumentException("The control must have a tag specifying a positive max value. Use 0 for no max value"); }
			catch (System.OverflowException) { throw new ArgumentException("The control must have a tag specifying a positive max value. Use 0 for no max value"); }
			 
			bool reject = false;
			
			string inputText = "";
			if (char.IsDigit(e.KeyChar))
			{
				inputText = e.KeyChar.ToString();
			}
			else if (e.KeyChar == 22 /*Ctrl-V*/)
			{
				if (System.Windows.Forms.Clipboard.ContainsText())
				{
					inputText = System.Windows.Forms.Clipboard.GetText();
					if (inputText.Length > control.MaxLength)
						inputText = inputText.Substring(0, control.MaxLength);
				}
				else reject = true;
			}
			else reject = (e.KeyChar != '\b' && e.KeyChar != 3 /*Ctrl-C*/ && e.KeyChar != 24 /*Ctrl-X*/);

			if (inputText != "")
			{
				string text = control.Text.Remove(control.SelectionStart, control.SelectionLength)
							.Insert(control.SelectionStart, inputText);

				try {
					Int32 value = Convert.ToInt32(text.Substring(0, Math.Min(text.Length, control.MaxLength)));
					if (value > maxValue || value <= 0)
						reject = true;
				}
				catch (System.FormatException) { reject = true; }
				catch (System.OverflowException) { reject = true; }
			}

			if (reject)
			{
				e.Handled = true;
				System.Media.SystemSounds.Exclamation.Play();
			}
		}

		private void MainWindow_FormClosing(object sender, FormClosingEventArgs e)
		{

		}

		private void serverStartButton_Click(object sender, EventArgs e)
		{
			StreamReader stdout;
			ushort port, fps;
			try
			{
				port = (ushort)Convert.ToInt32(portInput.Text);
				fps = (ushort)Convert.ToInt32(fpsInput.Text);
			}
			catch (SystemException)
			{
				connectionStatusLabel.Text = "Invalid values for port or fps";
				return;
			}
			System.Diagnostics.Process server = ServerLauncher.launch(port, fps, out stdout);
			new Thread(new ThreadStart(() =>
			{
				string output;
				while ((output = stdout.ReadLine()) != null)
				{
					serverOutput.WriteLine(output);
				}
				serverOutput.WriteLine("Failed to start server.");
				stdout.Close();
			}
			)).Start();
			serverStartButton.Text = "Stop";
			fpsInput.Enabled = false;
			portInput.Enabled = false;
			server.WaitForExit(500);
			if (server.HasExited)
			{
				connectionStatusLabel.Text = "Failed to start server on port " + Convert.ToInt32(portInput.Text);
				return;
			}

			connectionStatusLabel.Text = "Connecting to localhost:" + port + "...";
			connectionProgressBar.Visible = true;
			connectionProgressBar.Value = 0;
			connectToServer.RunWorkerAsync(port);
		}

		private ServerConnection conn = new ServerConnection();
		private void connectToServer_DoWork(object sender, DoWorkEventArgs e)
		{
			BackgroundWorker worker = sender as BackgroundWorker;
			worker.WorkerReportsProgress = true;
			bool success = false;

			for (int i = 1; i <= 10 && !success; i++)
			{
				if (worker.CancellationPending)
				{
					e.Cancel = true;
					break;
				}
				else
				{
					success = conn.Connect((ushort)e.Argument, TimeSpan.FromMilliseconds(300));
					worker.ReportProgress(i * 10);
				}
			}

			e.Result = success;
		}

		private void connectToServer_ProgressChanged(object sender, ProgressChangedEventArgs e)
		{
			connectionProgressBar.Value = e.ProgressPercentage;
		}

		private void connectToServer_RunWorkerCompleted(object sender, RunWorkerCompletedEventArgs e)
		{
			if (e.Cancelled || !(bool)e.Result)
			{
				connectionStatusLabel.Text = "Failed to start server on port " + Convert.ToInt32(portInput.Text);
				serverOutput.WriteLine("Failed to start server.");
			}
			else
			{
				connectionStatusLabel.Text = "Connected to localhost:" + Convert.ToInt32(portInput.Text);
				serverOutput.WriteLine("Server started succesfully.");
			}
			connectionProgressBar.Visible = false;
		}

	}

	public static class TextBoxExtension
	{
		internal static void WriteLine(this TextBox console, string Line)
		{
			if (!console.Multiline)
				throw new ArgumentException("The TextBox must be multi line.");

			Action action = delegate() { console.AppendText(Line); console.AppendText(Environment.NewLine); };
			if (console.InvokeRequired)
				console.Invoke(action);
			else { console.AppendText(Line); console.AppendText(Environment.NewLine); }
		}
	}
}

