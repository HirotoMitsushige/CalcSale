package jp.alhinc.mitsushige_hiroto.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class OpenOfficeFile{


	public static void main(String[] args) throws IOException {

		BufferedReader br=null;
		//コマンドライン引数が無いか2つ以上の場合処理を終了。
		if(args.length==0||args.length>=2){
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		//集計用
		HashMap<String,Long>branchSaleMap=new HashMap<String,Long>();
		HashMap<String,Long>commoditySaleMap=new HashMap<String,Long>();
		HashMap<String,String>commodity=new HashMap<String,String>();
		HashMap<String,String>branch=new HashMap<String,String>();
		ArrayList<String> rcdList=new ArrayList<String>();
		ArrayList<Integer> notRcdList=new ArrayList<Integer>();

		//支店定義ファイル----------------------------------------------------
		//blanch.lst
		if(!(readSaleFile(args[0],"branch.lst","^\\d{3}",
				"支店定義ファイルのフォーマットが不正です",branch,branchSaleMap))){
			return;
		}
		if(!(readSaleFile(args[0],"commodity.lst","^\\w{8}",
				"商品定義ファイルのフォーマットが不正です",commodity,commoditySaleMap))){
			return;
		}
		//集計------------------------------------------------------------------------
		try{
			File filler=new File(args[0]);
			String[] fileList=filler.list();
			for(int i=0;i<fileList.length;i++){
				if(fileList[i].matches("\\d{8}.rcd$")){
					rcdList.add(fileList[i]);
					String[] data=fileList[i].split("\\.");
					notRcdList.add(Integer.parseInt(data[0]));
				}
			}
			//連番処理はList化し、最大値-最小値＋1する。
			if(!(notRcdList.size()==(notRcdList.get(notRcdList.size()-1))-(notRcdList.get(0))+1)){
				System.out.println("売上げファイル名が連番になっていません");
				return;
			}
			Long Sale=null;

			for(int i = 0; i<rcdList.size(); i++){
				ArrayList<String> rcd=new ArrayList<String>();
				File file=new File(args[0],rcdList.get(i));

				br=new BufferedReader(new FileReader(file));
				String s;
				while((s=br.readLine())!=null){
					rcd.add(s);
				}

				//不正処理
				if(!(rcd.size()==3)){
					System.out.println(rcdList.get(i)+"のフォーマットが不正です");
					return;
				}
				if(!branchSaleMap.containsKey(rcd.get(0))){
					System.out.println(rcdList.get(i)+"の支店コードが不正です");
					return;
				}
				if(!commodity.containsKey(rcd.get(1))){
					System.out.println(rcdList.get(i)+"の商品コードが不正です");
					return;
				}

				Sale = branchSaleMap.get(rcd.get(0)) + Long.parseLong(rcd.get(2));
				branchSaleMap.put(rcd.get(0),Sale);
				commoditySaleMap.put(rcd.get(1),Sale);

				//金額が10桁を超えたら
				if(Sale<1000000000L){
				}else{
					System.out.println("合計金額が10桁を超えました");
					return;
				}
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
		}finally{
			try{
				if(br!=null)
					br.close();
			}catch(IOException e){
				System.out.println("予期せぬエラーが発生しました");
			}
		}
		//支店別集計ファイルと商品別集計ファイル----------------------------------------------------------------------

		if(!(writerFile(args[0],"branch.out",branchSaleMap,branch))){
			return;
		}
		if(!(writerFile(args[0],"commodity.out",commoditySaleMap,commodity))){
			return;
		}
	}
	//ファイルチェック------------------------------------------------------------------------------------------------
	private static boolean checkFile(File file){
		if (file.isFile()&&file.canRead()){
			return true;
		}
		return false;
	}
	//支店定義ファイルと商品定義ファイル------------------------------------------------------------------------------
	public static boolean readSaleFile(String comLine,String fileLst,String comLineVoid,String errMessage,HashMap<String, String> nameMap,HashMap<String, Long>sales){
		BufferedReader br=null;
		try{
			//ファイルを読み込む際は区切りに「￥」を使用するとWindowsのみで動く
			//仕様となるため、「,」で区切りを入れ、ファイルを読み込む。
			File file=new File(comLine,fileLst);
			br=new BufferedReader(new FileReader(file));
			String str=null;

			//ファイルが読み込めるかチェックする
			if(checkFile(file)){
				while((str=br.readLine())!=null){
					String[] data=str.split(",");

					//data.lengthで要素数を取得し、要素数が2でない場合はelse(「,」を含む文字列はＮＧ)
					if(!(data[0].matches(comLineVoid)&&(data[1].matches("\\S+"))&&data.length==2)){
						System.out.println(errMessage);
						return false;
					}
					if(!(data[0].matches(comLineVoid)&&(data[1].matches("\\S+"))&&data.length==2)){
						System.out.println(errMessage);
						return false;
					}
					nameMap.put(data[0],data[1]);
					sales.put(data[0],0L);
				}
			}else{
				System.out.println("ファイルを読み込めません");
			}
		}catch(FileNotFoundException e){
			System.out.println("支店定義ファイルが見つかりません");
			return false;
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println("支店定義ファイルのフォーマットが不正です");
			return false;
		}
		//try文においてfinallyは必ず読み込まれるためここでbrを閉じる。
		finally{
			if(br!=null)
				try{
					br.close();
					return true;
				}catch(IOException e){
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
		return false;
	}
	//集計ファイル処理--------------------------------------------------------------------------------------
	public static boolean writerFile(String dirpath,String fileName ,HashMap<String, Long> sales,HashMap<String, String>nameMap){

		BufferedWriter bw=null;
		try{
			File file = new File(dirpath, fileName);

			bw=new BufferedWriter(new FileWriter(file));
			List<Map.Entry<String,Long>> entries =
					new ArrayList<Map.Entry<String,Long>>(sales.entrySet());
			Collections.sort(entries, new Comparator<Map.Entry<String,Long>>() {

				public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
					return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
				}
			});
			for (Entry<String,Long> o : entries) {

				bw.write(o.getKey()+","+nameMap.get(o.getKey())+","+o.getValue());
				bw.newLine();
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}finally{
			if(bw!=null)
				try{
					bw.close();
					return true;
				}catch(IOException e){
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
		return false;
	}
}
