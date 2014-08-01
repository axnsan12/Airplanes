using System;
using System.Diagnostics;
using System.Runtime.InteropServices;

namespace console
{
	class ServerLauncher
	{
		static ServerLauncher()
		{
			hJob = new JobHandle();
			hJob.Handle = CreateJobObject(IntPtr.Zero, null);

			var info = new JOBOBJECT_BASIC_LIMIT_INFORMATION
			{
				LimitFlags = JOBOBJECTLIMIT.KillOnJobClose
			};

			var extendedInfo = new JOBOBJECT_EXTENDED_LIMIT_INFORMATION
			{
				BasicLimitInformation = info
			};

			int length = Marshal.SizeOf(typeof(JOBOBJECT_EXTENDED_LIMIT_INFORMATION));
			IntPtr extendedInfoPtr = Marshal.AllocHGlobal(length);
			Marshal.StructureToPtr(extendedInfo, extendedInfoPtr, false);

			if (!SetInformationJobObject(hJob.Handle, JOBOBJECTINFOCLASS.ExtendedLimitInformation, extendedInfoPtr, (uint)length)) {
				Marshal.FreeHGlobal(extendedInfoPtr);
				throw new Exception(string.Format("Error {0}", Marshal.GetLastWin32Error()));
			}

			Marshal.FreeHGlobal(extendedInfoPtr);
		}

		public static Process testproc;
		public static Process launch(ushort port, ushort fps, out System.IO.StreamReader stdout)
		{
			ProcessStartInfo processStartInfo = new ProcessStartInfo(
				"..\\..\\..\\Release\\airplanes-server.exe",
				String.Format("-port:{0} -fps:{1}", port, fps));
			processStartInfo.UseShellExecute = false;
			processStartInfo.ErrorDialog = false;
			processStartInfo.RedirectStandardOutput = true;
			processStartInfo.CreateNoWindow = true;
			Process process = new Process();
			process.StartInfo = processStartInfo;
			bool processStarted = process.Start();
			stdout = process.StandardOutput;

			AssignProcessToJobObject(hJob.Handle, process.Handle);
			return process;
		}

		private sealed class JobHandle
		{
			public JobHandle()
			{
				Handle = IntPtr.Zero;
			}

			public IntPtr Handle { get; set; }

			~JobHandle()
			{
				if (Handle != IntPtr.Zero)
					CloseHandle(Handle);
			}
		}
		private static JobHandle hJob;

		[DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
		private static extern IntPtr CreateJobObject(IntPtr a, string lpName);

		[DllImport("kernel32.dll")]
		private static extern bool SetInformationJobObject(IntPtr hJob, JOBOBJECTINFOCLASS infoType, IntPtr lpJobObjectInfo, UInt32 cbJobObjectInfoLength);

		[DllImport("kernel32.dll", SetLastError = true)]
		private static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);

		[DllImport("kernel32.dll", SetLastError = true)]
		[return: MarshalAs(UnmanagedType.Bool)]
		private static extern bool CloseHandle(IntPtr hObject);

		[StructLayout(LayoutKind.Sequential)]
		private struct JOBOBJECT_EXTENDED_LIMIT_INFORMATION
		{
			public JOBOBJECT_BASIC_LIMIT_INFORMATION BasicLimitInformation;
			public IO_COUNTERS IoInfo;
			public UIntPtr ProcessMemoryLimit;
			public UIntPtr JobMemoryLimit;
			public UIntPtr PeakProcessMemoryUsed;
			public UIntPtr PeakJobMemoryUsed;
		}

		[StructLayout(LayoutKind.Sequential)]
		private struct JOBOBJECT_BASIC_LIMIT_INFORMATION
		{
			public Int64 PerProcessUserTimeLimit;
			public Int64 PerJobUserTimeLimit;
			public JOBOBJECTLIMIT LimitFlags;
			public UIntPtr MinimumWorkingSetSize;
			public UIntPtr MaximumWorkingSetSize;
			public UInt32 ActiveProcessLimit;
			public Int64 Affinity;
			public UInt32 PriorityClass;
			public UInt32 SchedulingClass;
		}

		[Flags]
		private enum JOBOBJECTLIMIT : uint
		{
			// Basic Limits
			Workingset = 0x00000001,
			ProcessTime = 0x00000002,
			JobTime = 0x00000004,
			ActiveProcess = 0x00000008,
			Affinity = 0x00000010,
			PriorityClass = 0x00000020,
			PreserveJobTime = 0x00000040,
			SchedulingClass = 0x00000080,

			// Extended Limits
			ProcessMemory = 0x00000100,
			JobMemory = 0x00000200,
			DieOnUnhandledException = 0x00000400,
			BreakawayOk = 0x00000800,
			SilentBreakawayOk = 0x00001000,
			KillOnJobClose = 0x00002000,
			SubsetAffinity = 0x00004000,

			// Notification Limits
			JobReadBytes = 0x00010000,
			JobWriteBytes = 0x00020000,
			RateControl = 0x00040000,
		}

		[StructLayout(LayoutKind.Sequential)]
		private struct IO_COUNTERS
		{
			public UInt64 ReadOperationCount;
			public UInt64 WriteOperationCount;
			public UInt64 OtherOperationCount;
			public UInt64 ReadTransferCount;
			public UInt64 WriteTransferCount;
			public UInt64 OtherTransferCount;
		}

		private enum JOBOBJECTINFOCLASS : uint
		{
			AssociateCompletionPortInformation = 7,
			BasicLimitInformation = 2,
			BasicUIRestrictions = 4,
			EndOfJobTimeInformation = 6,
			ExtendedLimitInformation = 9,
			SecurityLimitInformation = 5,
			GroupInformation = 11
		}
	}
}
