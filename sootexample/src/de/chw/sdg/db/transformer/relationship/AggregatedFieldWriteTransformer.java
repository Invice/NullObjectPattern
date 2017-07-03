package de.chw.sdg.db.transformer.relationship;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import de.chw.sdg.db.RelTypes;

public class AggregatedFieldWriteTransformer extends GenericRelationshipTransformer {

	public AggregatedFieldWriteTransformer() {
		super(RelTypes.AGGREGATED_FIELD_WRITE);
	}

	public Relationship transform(final Node sourceNode, final Node targetNode, final int amount) {
		Relationship relationship = super.transform(sourceNode, targetNode);
		relationship.setProperty("amount", amount);
		return relationship;
	}
}
