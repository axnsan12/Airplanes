using System;
using System.Windows.Forms;
using System.Diagnostics;
using System.IO;
using System.Threading;

namespace console
{
	static class Program
	{
		

		[STAThread]
		static void Main()
		{
			Application.EnableVisualStyles();
			Application.SetCompatibleTextRenderingDefault(false);
			Application.Run(new MainWindow());
		}
	}
}
