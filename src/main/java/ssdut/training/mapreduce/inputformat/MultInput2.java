package ssdut.training.mapreduce.inputformat;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MultInput2 {

	public static class TxtFileMapper extends Mapper<Object, Text, Text, IntWritable> {		
		private final static IntWritable one = new IntWritable(1);
		
		public void map(Object key, Text value, Context context ) 
				throws IOException, InterruptedException {
	    	String[] strs = value.toString().split(" ");	//按空格分割输入
	    	Text date = new Text(strs[0]);
			context.write(date, one);	
	    }
	}
	
	public static class CsvFileMapper extends Mapper<Object, Text, Text, IntWritable> {		
		private final static IntWritable one = new IntWritable(1);
		
		public void map(Object key, Text value, Context context ) 
				throws IOException, InterruptedException {
	    	String[] strs = value.toString().split(",");	//按逗号分割输入
	    	Text date = new Text(strs[0]);
			context.write(date, one);	
	    }
	}
  
	public static class MultInput2Reducer extends Reducer<Text,IntWritable,Text,IntWritable> {
		public void reduce(Text key, Iterable<IntWritable> values, Context context) 
				throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values) {
				sum += val.get();
			}
			context.write(key, new IntWritable(sum));
	    }
	}

	public static void main(String[] args) throws Exception {		
		//1.设置HDFS配置信息
		String namenode_ip = "192.168.17.10";
		String hdfs = "hdfs://" + namenode_ip + ":9000";			
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", hdfs);
		conf.set("mapreduce.app-submission.cross-platform", "true");

		//2.设置MapReduce作业配置信息
		String jobName = "MultInput2";					//作业名称
		Job job = Job.getInstance(conf, jobName);
		job.setJarByClass(MultInput2.class);			//指定运行时作业类
		job.setJar("export\\MultInput2.jar");			//指定本地jar包
		
		//job.setMapperClass(MultInput2Mapper.class);	//无需指定Mapper类，而在MultipleInputs.addInputPath()方法中指定
		job.setMapOutputKeyClass(Text.class);			//设置Mapper输出Key类型
		job.setMapOutputValueClass(IntWritable.class);	//设置Mapper输出Value类型
		job.setReducerClass(MultInput2Reducer.class);	//指定Reducer类
		job.setOutputKeyClass(Text.class);				//设置Reduce输出Key类型
		job.setOutputValueClass(IntWritable.class); 	//设置Reduce输出Value类型
		
		//3.设置作业输入和输出路径	
		//方法五：MultipleInputs.addInputPath()
		MultipleInputs.addInputPath(job, new Path(hdfs+"/expr/multinput/data/txt1"), TextInputFormat.class, TxtFileMapper.class);
		MultipleInputs.addInputPath(job, new Path(hdfs+"/expr/multinput/data/csv"), TextInputFormat.class, CsvFileMapper.class);
		
		Path outPath = new Path(hdfs + "/expr/multinput/output3");		//输出目录
		FileOutputFormat.setOutputPath(job, outPath);
		FileSystem fs = FileSystem.get(conf);
		if(fs.exists(outPath)) {
			fs.delete(outPath, true);
		}
		
		//4.运行作业
		System.out.println("Job: " + jobName + " is running...");
		if(job.waitForCompletion(true)) {
			System.out.println("success!");
			System.exit(0);
		} else {
			System.out.println("failed!");
			System.exit(1);
		}
	}

}