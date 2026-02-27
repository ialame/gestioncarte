package com.pcagrade.retriever.card.image;

import com.pcagrade.retriever.card.Card;
import com.pcagrade.retriever.card.image.history.CardImageHistory;
import com.pcagrade.mason.ulid.jpa.AbstractUlidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "card_image")
public class CardImage extends AbstractUlidEntity {

	@ManyToOne
	@JoinColumn(name = "card_id")
	private Card card;

	@OneToMany(mappedBy = "image", cascade = CascadeType.ALL)
	private List<CardImageHistory> history;


	public Card getCard() {
		return card;
	}

	public void setCard(Card card) {
		this.card = card;
	}

	public List<CardImageHistory> getHistory() {
		return history;
	}

	public void setHistory(List<CardImageHistory> history) {
		this.history = history;
	}
}
