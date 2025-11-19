package server.watchlist.data;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseObject implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3539963177093394568L;
	private Data data = new Data(new Data.Media(0, 0, new String[] {}, new Data.Media.Title("", ""), new Data.Media.CoverImage(""), ""));
	private boolean invalid = false;
	
	@JsonCreator
	public ResponseObject(@JsonProperty("data") Data data) {
		this.data = data;
	}
	
	public ResponseObject(int id) {
		data.media.id = id;
		invalid = true;
	}
	
	public int getId() {
		return data.media.id;
	}
	
	public int getEpisodes() {
		return data.media.episodes;
	}
	
	public String[] getGenres() {
		if(data.media.genres == null)
			return new String[0];
		return data.media.genres;
	}
	
	public String getRomaji() {
		if(data.media.title.romaji == null) {
			if(data.media.title.english == null)
				return "#" + data.media.id;
			return data.media.title.english;
		}
		return data.media.title.romaji;
	}
	
	public String getEnglish() {
		if(data.media.title.english == null) {
			if(data.media.title.romaji == null)
				return "#" + data.media.id;
			return data.media.title.romaji;
		}
		return data.media.title.english;
	}
	
	public String getMedium() {
		if(data.media.coverImage.medium == null)
			return "https://jatheon.com/wp-content/uploads/2018/03/404-image.png";
		return data.media.coverImage.medium;
	}
	
	public String getSiteUrl() {
		return data.media.siteUrl;
	}
	
	public boolean isValid() {
		return !invalid;
	}
	
	public static class Data implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6262576760479596697L;
		private Media media;
		
		@JsonCreator
		public Data(@JsonProperty("Media") Media media) {
			this.media = media;
		}
		
		public static class Media implements Serializable {
			/**
			 * 
			 */
			private static final long serialVersionUID = -5495533927659279315L;
			private int id;
			private int episodes;
			private String[] genres;
			private Title title;
			private CoverImage coverImage;
			private String siteUrl;
			
			@JsonCreator
			public Media(@JsonProperty("id") int id, @JsonProperty("episodes") int episodes, @JsonProperty("genres") String[] genres, @JsonProperty("title") Title title, @JsonProperty("coverImage") CoverImage coverImage, @JsonProperty("siteUrl") String siteUrl) {
				this.id = id;
				this.episodes = episodes;
				this.genres = genres;
				this.title = title;
				this.coverImage = coverImage;
				this.siteUrl = siteUrl;
			}
			
			public static class Title implements Serializable {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1219022509459933175L;
				private String romaji;
				private String english;
				
				@JsonCreator
				public Title(@JsonProperty("romaji") String romaji, @JsonProperty("english") String english) {
					this.romaji = romaji;
					this.english = english;
				}
			}
			
			public static class CoverImage implements Serializable {
				/**
				 * 
				 */
				private static final long serialVersionUID = 1455375996747130162L;
				private String medium;
				
				@JsonCreator
				public CoverImage(@JsonProperty("medium") String medium) {
					this.medium = medium;
				}
			}
		}
	}
}
