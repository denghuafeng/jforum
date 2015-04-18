/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.entities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.jforum.view.forum.common.PostCommon;

/**
 * A helper class that holds changes made to the pool.
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class PollChanges {
	private List<PollOption> deletedOptions = new ArrayList<PollOption>();
	private List<PollOption> newOptions = new ArrayList<PollOption>();
	private List<PollOption> changedOptions = new ArrayList<PollOption>();
	
	private boolean hasChanges;
	
	private Poll first;
	private Poll second;
	
	/**
	 * @param first The "complete", most recent poll version. Usually the one
	 * that's in the database. 
	 * @param second The poll to compare with. It usually will be a poll filled
	 * by {@link PostCommon#fillPostFromRequest()}, so matches will be done againts the 
	 * existing poll and the data comming from the server. 
	 */
	public PollChanges(Poll first, Poll second) {
		this.first = first;
		this.second = second;
	}
	
	public void addChangedOption(PollOption option) {
		this.changedOptions.add(option);
		this.hasChanges = true;
	}
	
	public List<PollOption> getChangedOptions() {
		return this.changedOptions;
	}
	
	public void addDeletedOption(PollOption option) {
		this.deletedOptions.add(option);
		this.hasChanges = true;
	}

	public List<PollOption> getDeletedOptions() {
		return this.deletedOptions;
	}
	
	public void addNewOption(PollOption option) {
		this.newOptions.add(option);
		this.hasChanges = true;
	}

	public List<PollOption> getNewOptions() {
		return this.newOptions;
	}
	
	public boolean hasChanges() {
		this.searchForChanges();
		return this.hasChanges;
	}
	
	private void searchForChanges() {
		if (first == null || second == null) {
			return;
		}
		
		boolean isSame = first.getLabel().equals(second.getLabel());
		isSame &= first.getLength() == second.getLength();
		
		this.hasChanges = !isSame;
		
		List<PollOption> firstOptions = first.getOptions();
		List<PollOption> secondOptions = second.getOptions();
		
		// Search for changes in existing options
		for (Iterator<PollOption> iter = firstOptions.iterator(); iter.hasNext(); ) {
			PollOption option = iter.next();
			PollOption changed = this.findOptionById(option.getId(), secondOptions);
			
			if (changed != null && !option.getText().equals(changed.getText())) {
				this.addChangedOption(changed);
			}
			else if (changed == null) {
				this.addDeletedOption(option);
			}
		}

		// Check if the incoming poll added options
		for (Iterator<PollOption> iter = secondOptions.iterator(); iter.hasNext(); ) {
			PollOption option = iter.next();
			
			if (this.findOptionById(option.getId(), firstOptions) == null) {
				this.addNewOption(option);
			}
		}
	}
	
	private PollOption findOptionById(int id, List<PollOption> options) {
		for (Iterator<PollOption> iter = options.iterator(); iter.hasNext(); ) {
			PollOption o = iter.next();
			
			if (o.getId() == id) {
				return o;
			}
		}
		
		return null;
	}
}