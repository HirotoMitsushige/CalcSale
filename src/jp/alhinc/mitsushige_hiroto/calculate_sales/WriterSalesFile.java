package jp.alhinc.mitsushige_hiroto.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

public class WriterSalesFile{


	public static void main(String[] args) {

		BufferedReader br = null;
		//コマンドライン引数が無いか2つ以上の場合処理を終了。
		if(args.length != 1){
			System.out.println("予期せぬエラーが発生しました");
			return;
		}
		HashMap<String,Long> branchSaleMap = new HashMap<>();
		HashMap<String,Long> commoditySaleMap = new HashMap<>();
		HashMap<String,String> commodityMap = new HashMap<>();
		HashMap<String,String> branchMap = new HashMap<>();
		ArrayList<String> rcdList = new ArrayList<>();
		ArrayList<Integer> rcdNameList = new ArrayList<>();

		//支店、商品定義ファイル----------------------------------------------------
		if(!(readSaleFile(args[0],"branch.lst" , "^\\d{3}",
				"支店" , branchMap , branchSaleMap))){
			return;
		}
		if(!(readSaleFile(args[0] , "commodity.lst" , "^\\w{8}",
				"商品" , commodityMap , commoditySaleMap))){
			return;
		}
		//集計------------------------------------------------------------------------
		try{
			File filler = new File(args[0]);
			String[] fileList = filler.list();
			for(int i = 0; i < fileList.length; i++){
				if(checkFile(filler)&&fileList[i].matches("\\d{8}.rcd$")){
					rcdList.add(fileList[i]);
					String[] data = fileList[i].split("\\.");
					rcdNameList.add(Integer.parseInt(data[0]));
				}
			}
			//連番処理はList化し、最大値-最小値＋1する。
			if(rcdNameList.size() != rcdNameList.get(rcdNameList.size() -1) - rcdNameList.get(0) + 1){
				System.out.println("売上ファイル名が連番になっていません");
				return;
			}
			Long branchsale = null;
			Long commoditysale = null;

			for(int i = 0; i< rcdList.size(); i++){
				ArrayList<String> rcd = new ArrayList<>();
				File file = new File(args[0],rcdList.get(i));

				br = new BufferedReader(new FileReader(file));
				String s;
				while((s = br.readLine()) != null){
					rcd.add(s);
				}

				//フォーマット不正処理
				if(rcd.size() != 3){
					System.out.println(rcdList.get(i) + "のフォーマットが不正です");
					return;
				}
				if(!branchSaleMap.containsKey(rcd.get(0))){
					System.out.println(rcdList.get(i) + "の支店コードが不正です");
					return;
				}
				if(!commodityMap.containsKey(rcd.get(1))){
					System.out.println(rcdList.get(i) + "の商品コードが不正です");
					return;
				}
				if(!rcd.get(2).matches("\\d+")){
					System.out.println("予期せぬエラーが発生しました");
					return;
				}

				branchsale = branchSaleMap.get(rcd.get(0)) + Long.parseLong(rcd.get(2));
				commoditysale = commoditySaleMap.get(rcd.get(1)) + Long.parseLong(rcd.get(2));
				//金額が10桁を超えたら
				if(branchsale > 999999999L){
					System.out.println("合計金額が10桁を超えました");
					return;
				}
				if(commoditysale > 999999999L){
					System.out.println("合計金額が10桁を超えました");
					return;
				}

				branchSaleMap.put(rcd.get(0),branchsale);
				commoditySaleMap.put(rcd.get(1),commoditysale);
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
		}finally{
			try{
				if(br != null)
					br.close();
			}catch(IOException e){
				System.out.println("予期せぬエラーが発生しました");
			}
		}
		//支店、商品別集計ファイル----------------------------------------------------------------------
		if(!(writerFile(args[0] , "branch.out"  ,branchSaleMap , branchMap))){
			return;
		}
		if(!(writerFile(args[0] , "commodity.out" , commoditySaleMap , commodityMap))){
			return;
		}
	}
	//支店、商品定義ファイル------------------------------------------------------------------------------
	public static boolean readSaleFile(String dirpath , String fileLst , String comLineVoid , String errMessage ,
			HashMap<String, String> nameMap , HashMap<String, Long> sales){
		BufferedReader br = null;
		try{
			File file = new File(dirpath,fileLst);
			if(!checkFile(file)){
				System.out.println(errMessage+"定義ファイルが存在しません");
				return false;
			}
			br = new BufferedReader(new FileReader(file));
			String str = null;

			while((str = br.readLine()) != null){
				String[] data = str.split(",");

				if(!(data[0].matches(comLineVoid) && (data[1].matches("\\S+")) && data.length == 2)){
					System.out.println(errMessage + "定義ファイルのフォーマットが不正です");
					return false;
				}
				nameMap.put(data[0],data[1]);
				sales.put(data[0],0L);
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}catch(ArrayIndexOutOfBoundsException e){
			System.out.println(errMessage + "定義ファイルのフォーマットが不正です");
			return false;
		}
		//try文においてfinallyは必ず読み込まれるためここでbrを閉じる。
		finally{
			if(br != null)
				try{
					br.close();
				}catch(IOException e){
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
		return true;
	}
	//集計ファイル処理--------------------------------------------------------------------------------------
	public static boolean writerFile(String dirpath , String fileName ,
			HashMap<String, Long> sales , HashMap<String, String> nameMap){

		BufferedWriter bw = null;
		try{
			File file = new File(dirpath, fileName);
			bw = new BufferedWriter(new FileWriter(file));
			List<Map.Entry<String,Long>> entries =
					new ArrayList<Map.Entry<String,Long>>(sales.entrySet());

			Collections.sort(entries, new Comparator<Map.Entry<String,Long>>() {
				public int compare(Entry<String,Long> entry1, Entry<String,Long> entry2) {
					return ((Long)entry2.getValue()).compareTo((Long)entry1.getValue());
				}
			});
			for (Entry<String,Long> o : entries) {
				bw.write(o.getKey() + "," + nameMap.get(o.getKey()) + "," + o.getValue());
				bw.newLine();
			}
		}catch(IOException e){
			System.out.println("予期せぬエラーが発生しました");
			return false;
		}finally{
			if(bw != null)
				try{
					bw.close();
				}catch(IOException e){
					System.out.println("予期せぬエラーが発生しました");
					return false;
				}
		}
		return true;
	}
	 //ファイルチェック
    private static boolean checkFile(File file){
             if (file.isFile() && file.canRead() && file.exists()){
                     return true;
             }
             return false;
     }

}
