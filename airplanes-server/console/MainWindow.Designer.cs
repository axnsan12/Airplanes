namespace console
{
	partial class MainWindow
	{
		/// <summary>
		/// Required designer variable.
		/// </summary>
		private System.ComponentModel.IContainer components = null;

		/// <summary>
		/// Clean up any resources being used.
		/// </summary>
		/// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
		protected override void Dispose(bool disposing)
		{
			if (disposing && (components != null))
			{
				components.Dispose();
			}
			base.Dispose(disposing);
		}

		#region Windows Form Designer generated code

		/// <summary>
		/// Required method for Designer support - do not modify
		/// the contents of this method with the code editor.
		/// </summary>
		private void InitializeComponent()
		{
			this.statusStrip1 = new System.Windows.Forms.StatusStrip();
			this.connectionStatusLabel = new System.Windows.Forms.ToolStripStatusLabel();
			this.connectionProgressBar = new System.Windows.Forms.ToolStripProgressBar();
			this.statusBarSpring = new System.Windows.Forms.ToolStripStatusLabel();
			this.toolStripStatusLabel3 = new System.Windows.Forms.ToolStripStatusLabel();
			this.serverOutput = new System.Windows.Forms.TextBox();
			this.tableLayoutPanel1 = new System.Windows.Forms.TableLayoutPanel();
			this.panel1 = new System.Windows.Forms.Panel();
			this.portInput = new System.Windows.Forms.TextBox();
			this.fpsInput = new System.Windows.Forms.TextBox();
			this.serverStartButton = new System.Windows.Forms.Button();
			this.label2 = new System.Windows.Forms.Label();
			this.label1 = new System.Windows.Forms.Label();
			this.connectToServer = new System.ComponentModel.BackgroundWorker();
			this.clientListbox = new System.Windows.Forms.ListBox();
			this.statusStrip1.SuspendLayout();
			this.tableLayoutPanel1.SuspendLayout();
			this.panel1.SuspendLayout();
			this.SuspendLayout();
			// 
			// statusStrip1
			// 
			this.statusStrip1.Items.AddRange(new System.Windows.Forms.ToolStripItem[] {
            this.connectionStatusLabel,
            this.connectionProgressBar,
            this.statusBarSpring,
            this.toolStripStatusLabel3});
			this.statusStrip1.Location = new System.Drawing.Point(0, 324);
			this.statusStrip1.Name = "statusStrip1";
			this.statusStrip1.Size = new System.Drawing.Size(554, 22);
			this.statusStrip1.TabIndex = 0;
			this.statusStrip1.Text = "statusStrip1";
			// 
			// connectionStatusLabel
			// 
			this.connectionStatusLabel.Name = "connectionStatusLabel";
			this.connectionStatusLabel.Size = new System.Drawing.Size(118, 17);
			this.connectionStatusLabel.Text = "toolStripStatusLabel1";
			// 
			// connectionProgressBar
			// 
			this.connectionProgressBar.Name = "connectionProgressBar";
			this.connectionProgressBar.Size = new System.Drawing.Size(100, 16);
			this.connectionProgressBar.Visible = false;
			// 
			// statusBarSpring
			// 
			this.statusBarSpring.Name = "statusBarSpring";
			this.statusBarSpring.Size = new System.Drawing.Size(303, 17);
			this.statusBarSpring.Spring = true;
			// 
			// toolStripStatusLabel3
			// 
			this.toolStripStatusLabel3.Name = "toolStripStatusLabel3";
			this.toolStripStatusLabel3.Size = new System.Drawing.Size(118, 17);
			this.toolStripStatusLabel3.Text = "toolStripStatusLabel3";
			// 
			// serverOutput
			// 
			this.serverOutput.AcceptsReturn = true;
			this.serverOutput.AcceptsTab = true;
			this.serverOutput.Dock = System.Windows.Forms.DockStyle.Fill;
			this.serverOutput.Location = new System.Drawing.Point(3, 31);
			this.serverOutput.Multiline = true;
			this.serverOutput.Name = "serverOutput";
			this.serverOutput.ReadOnly = true;
			this.serverOutput.ScrollBars = System.Windows.Forms.ScrollBars.Vertical;
			this.serverOutput.Size = new System.Drawing.Size(314, 283);
			this.serverOutput.TabIndex = 2;
			this.serverOutput.TabStop = false;
			// 
			// tableLayoutPanel1
			// 
			this.tableLayoutPanel1.Anchor = ((System.Windows.Forms.AnchorStyles)((((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Bottom) 
            | System.Windows.Forms.AnchorStyles.Left) 
            | System.Windows.Forms.AnchorStyles.Right)));
			this.tableLayoutPanel1.ColumnCount = 2;
			this.tableLayoutPanel1.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle(System.Windows.Forms.SizeType.Percent, 100F));
			this.tableLayoutPanel1.ColumnStyles.Add(new System.Windows.Forms.ColumnStyle(System.Windows.Forms.SizeType.Absolute, 210F));
			this.tableLayoutPanel1.Controls.Add(this.panel1, 0, 0);
			this.tableLayoutPanel1.Controls.Add(this.serverOutput, 0, 1);
			this.tableLayoutPanel1.Controls.Add(this.clientListbox, 1, 0);
			this.tableLayoutPanel1.Location = new System.Drawing.Point(12, 4);
			this.tableLayoutPanel1.Name = "tableLayoutPanel1";
			this.tableLayoutPanel1.RowCount = 2;
			this.tableLayoutPanel1.RowStyles.Add(new System.Windows.Forms.RowStyle());
			this.tableLayoutPanel1.RowStyles.Add(new System.Windows.Forms.RowStyle(System.Windows.Forms.SizeType.Percent, 100F));
			this.tableLayoutPanel1.Size = new System.Drawing.Size(530, 317);
			this.tableLayoutPanel1.TabIndex = 3;
			// 
			// panel1
			// 
			this.panel1.Controls.Add(this.portInput);
			this.panel1.Controls.Add(this.fpsInput);
			this.panel1.Controls.Add(this.serverStartButton);
			this.panel1.Controls.Add(this.label2);
			this.panel1.Controls.Add(this.label1);
			this.panel1.Dock = System.Windows.Forms.DockStyle.Fill;
			this.panel1.Location = new System.Drawing.Point(0, 0);
			this.panel1.Margin = new System.Windows.Forms.Padding(0);
			this.panel1.Name = "panel1";
			this.panel1.Size = new System.Drawing.Size(320, 28);
			this.panel1.TabIndex = 6;
			// 
			// portInput
			// 
			this.portInput.Location = new System.Drawing.Point(31, 4);
			this.portInput.MaxLength = 5;
			this.portInput.Name = "portInput";
			this.portInput.Size = new System.Drawing.Size(36, 20);
			this.portInput.TabIndex = 10;
			this.portInput.Tag = "65535";
			this.portInput.Text = "27015";
			this.portInput.TextChanged += new System.EventHandler(this.numericFilter_TextChanged);
			this.portInput.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.numericFilter_KeyPress);
			// 
			// fpsInput
			// 
			this.fpsInput.Location = new System.Drawing.Point(109, 4);
			this.fpsInput.MaxLength = 4;
			this.fpsInput.Name = "fpsInput";
			this.fpsInput.Size = new System.Drawing.Size(31, 20);
			this.fpsInput.TabIndex = 9;
			this.fpsInput.Tag = "1000";
			this.fpsInput.Text = "1000";
			this.fpsInput.TextChanged += new System.EventHandler(this.numericFilter_TextChanged);
			this.fpsInput.KeyPress += new System.Windows.Forms.KeyPressEventHandler(this.numericFilter_KeyPress);
			// 
			// serverStartButton
			// 
			this.serverStartButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
			this.serverStartButton.Location = new System.Drawing.Point(146, 2);
			this.serverStartButton.Name = "serverStartButton";
			this.serverStartButton.Size = new System.Drawing.Size(44, 25);
			this.serverStartButton.TabIndex = 8;
			this.serverStartButton.Text = "Start";
			this.serverStartButton.UseVisualStyleBackColor = true;
			this.serverStartButton.Click += new System.EventHandler(this.serverStartButton_Click);
			// 
			// label2
			// 
			this.label2.AutoSize = true;
			this.label2.Location = new System.Drawing.Point(83, 8);
			this.label2.Name = "label2";
			this.label2.Size = new System.Drawing.Size(30, 13);
			this.label2.TabIndex = 7;
			this.label2.Text = "FPS:";
			// 
			// label1
			// 
			this.label1.AutoSize = true;
			this.label1.Location = new System.Drawing.Point(3, 8);
			this.label1.Name = "label1";
			this.label1.Size = new System.Drawing.Size(29, 13);
			this.label1.TabIndex = 5;
			this.label1.Text = "Port:";
			// 
			// connectToServer
			// 
			this.connectToServer.DoWork += new System.ComponentModel.DoWorkEventHandler(this.connectToServer_DoWork);
			this.connectToServer.ProgressChanged += new System.ComponentModel.ProgressChangedEventHandler(this.connectToServer_ProgressChanged);
			this.connectToServer.RunWorkerCompleted += new System.ComponentModel.RunWorkerCompletedEventHandler(this.connectToServer_RunWorkerCompleted);
			// 
			// clientListbox
			// 
			this.clientListbox.Dock = System.Windows.Forms.DockStyle.Fill;
			this.clientListbox.FormattingEnabled = true;
			this.clientListbox.IntegralHeight = false;
			this.clientListbox.Items.AddRange(new object[] {
            "213.175.212.96 (reallylongnamehere)"});
			this.clientListbox.Location = new System.Drawing.Point(323, 3);
			this.clientListbox.Name = "clientListbox";
			this.tableLayoutPanel1.SetRowSpan(this.clientListbox, 2);
			this.clientListbox.ScrollAlwaysVisible = true;
			this.clientListbox.Size = new System.Drawing.Size(204, 311);
			this.clientListbox.TabIndex = 7;
			// 
			// MainWindow
			// 
			this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
			this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
			this.ClientSize = new System.Drawing.Size(554, 346);
			this.Controls.Add(this.tableLayoutPanel1);
			this.Controls.Add(this.statusStrip1);
			this.MinimumSize = new System.Drawing.Size(570, 385);
			this.Name = "MainWindow";
			this.Text = "Airplanes server interface";
			this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.MainWindow_FormClosing);
			this.statusStrip1.ResumeLayout(false);
			this.statusStrip1.PerformLayout();
			this.tableLayoutPanel1.ResumeLayout(false);
			this.tableLayoutPanel1.PerformLayout();
			this.panel1.ResumeLayout(false);
			this.panel1.PerformLayout();
			this.ResumeLayout(false);
			this.PerformLayout();

		}

		#endregion

		private System.Windows.Forms.StatusStrip statusStrip1;
		private System.Windows.Forms.ToolStripStatusLabel connectionStatusLabel;
		private System.Windows.Forms.ToolStripProgressBar connectionProgressBar;
		private System.Windows.Forms.ToolStripStatusLabel statusBarSpring;
		private System.Windows.Forms.ToolStripStatusLabel toolStripStatusLabel3;
		private System.Windows.Forms.TextBox serverOutput;
		private System.Windows.Forms.TableLayoutPanel tableLayoutPanel1;
		private System.Windows.Forms.Panel panel1;
		private System.Windows.Forms.Label label1;
		private System.Windows.Forms.Label label2;
		private System.Windows.Forms.Button serverStartButton;
		private System.Windows.Forms.TextBox fpsInput;
		private System.Windows.Forms.TextBox portInput;
		private System.ComponentModel.BackgroundWorker connectToServer;
		private System.Windows.Forms.ListBox clientListbox;
	}
}

