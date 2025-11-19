package server.watchlist.layouts;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import server.watchlist.data.AnimeEntry;
import server.watchlist.data.DataHandler;
import server.watchlist.data.User;
import server.watchlist.data.ResponseObject;

public class ResponseObjectLayoutList extends ArrayList<ResponseObjectLayout> implements Comparator<ResponseObjectLayout> {
	private String queryGenre = "";
	private String queryTitle = "";
	private String queryNote = "";
	private FilterModes[] filterModes = {};
	private SortingModes sortingMode = SortingModes.standard;
	private User user;
	private boolean changeButton;
	
	public ResponseObjectLayoutList(User user) {
		this(user, user.getList());
	}
	
	public ResponseObjectLayoutList(User user, List ae, boolean changeButton) {
		this.user = user;
		this.changeButton = changeButton;
		if(ae.size() > 0 && ae.get(0) instanceof AnimeEntry) {
			for(Object entry : ae) {
				this.add(new ResponseObjectLayout(DataHandler.getDetails(((AnimeEntry) entry).getId()), user, changeButton));
			}
		} else {
			for(Object ro : ae) {
				this.add(new ResponseObjectLayout((ResponseObject) ro, user, changeButton));
			}
		}
	}
	
	public ResponseObjectLayoutList(User user, List<AnimeEntry> ae) {
		this(user, ae, false);
	}
	
	public void setQueryTitle(String queryTitle) {
		this.queryTitle = queryTitle;
	}
	
	public void setQueryGenre(String queryGenre) {
		this.queryGenre = queryGenre;
	}
	
	public void setQueryNote(String queryNote) {
		this.queryNote = queryNote;
	}
	
	public void setFilterModes(FilterModes[] filterModes) {
		this.filterModes = filterModes;
	}
	
	public void setSortingMode(SortingModes sortingMode) {
		this.sortingMode = sortingMode;
	}
	
	private void filter() {
		for(FilterModes fm : filterModes) {
			for(int i = 0; i < this.size(); i++) {
			switch(fm) {
				case finished:
					if(!get(i).getAnimeEntry().getFinished())
						this.remove(get(i--));
					break;
				case genre:
					String str = "";
					for(String genre : get(i).getResponseObject().getGenres()) {
						str += genre;
					}
					str = str.toLowerCase();
					if(!str.contains(queryGenre.toLowerCase()))
						this.remove(get(i--));
					break;
				case note:
					if(!get(i).getAnimeEntry().getNote().toLowerCase().contains(queryNote.toLowerCase()))
						this.remove(get(i--));
					break;
				case title:
					if(!(get(i).getResponseObject().getEnglish().toLowerCase().contains(queryTitle.toLowerCase()) || get(i).getResponseObject().getRomaji().toLowerCase().contains(queryTitle.toLowerCase())))
						this.remove(get(i--));
					break;
				case unfinished:
					if(get(i).getAnimeEntry().getFinished())
						this.remove(get(i--));
					break;
				case watching:
					if(!get(i).getAnimeEntry().getWatching())
						this.remove(get(i--));
					break;
				case notWatching:
					if(get(i).getAnimeEntry().getWatching())
						this.remove(get(i--));
					break;
				}
			}
		}
	}
	
	public void addTo(VerticalLayout parent) {
		filter();
		ResponseObjectLayoutList nonWatching = stripNonWatching();
		this.sort(this);
		nonWatching.sort(this);
		this.addAll(nonWatching);
		for(int i = 0; i < this.size(); i++) {
			if(i % 2 == 0)
				this.get(i).getStyle().set("background-color", "#293d56");
			else
				this.get(i).getStyle().set("background-color", "#233348");
			this.get(i).getStyle().set("border-radius", "1em");
			parent.add(this.get(i));
		}
	}
	
	private ResponseObjectLayoutList stripNonWatching() {
		List<ResponseObject> nonWatching = new LinkedList<ResponseObject>();
		for(int i = 0; i < this.size(); i++) {
			if(!this.get(i).getAnimeEntry().getWatching()) {
				nonWatching.add(this.get(i).getResponseObject());
				this.remove(i--);
			}
		}
		return new ResponseObjectLayoutList(user, nonWatching, changeButton);
	}

	@Override
	public int compare(ResponseObjectLayout arg0, ResponseObjectLayout arg1) {
		if(arg0 == null || arg1 == null)
			throw new NullPointerException();
		switch(sortingMode) {
		case alphabet:
			String str = "";
			switch(user.getLang()) {
			case english:
				str = arg0.getResponseObject().getEnglish();
				break;
			case romaji:
				str = arg0.getResponseObject().getRomaji();
				break;
			}
			return str.compareToIgnoreCase(arg1.getResponseObject().getEnglish());
		case id:
			if(arg0.getResponseObject().getId() < arg1.getResponseObject().getId())
				return -1;
			if(arg0.getResponseObject().getId() > arg1.getResponseObject().getId())
				return 1;
			return 0;
		case standard:
			return 0;
		}
		return 0;
	}

	@Override
	public ResponseObjectLayoutList reversed() {
		return (ResponseObjectLayoutList) Comparator.super.reversed();
	}
}
