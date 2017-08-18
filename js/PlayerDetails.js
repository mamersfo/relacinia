import React from 'react';

import { graphql, createFragmentContainer } from 'react-relay';

const PlayerDetails = createFragmentContainer(
	({data}) => (
		<li key={data.id}>
		  {data.name} ({data.country.name})
		</li>
	),
	graphql`fragment PlayerDetails on Player { id, name, country { name } }`
);

export default PlayerDetails;
