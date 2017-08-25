import React from 'react';
import { graphql, createFragmentContainer } from 'react-relay';

class TeamOptions extends React.Component {

		render() {
				return (
						<div>
							<label>
								Team:
								<select>
									{this.props.data.map((team, i) => <option key={i} value={team.id}>{team.name}</option>)}
				        </select>
							</label>
						</div>
				);
		}
}

export default createFragmentContainer(TeamOptions,	graphql`
fragment TeamOptions on Team @relay(plural: true) { id, name }`
);
